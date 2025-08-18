package com.redelf.commons.media.player.wrapped.dev

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.asFlow
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.extensions.executeWithWorkManager
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@UnstableApi
@Deprecated("Just for the development purposes")
class ExoPlayerWorkManagerDataSource : DataSource {

    companion object {

        val DEBUG = AtomicBoolean(false)
        val VERBOSE = AtomicBoolean(false)
    }

    private var opened = false
    private var bytesRemaining: Long = 0
    private var inputStream: InputStream? = null
    private val tag = "Exo :: Worker Manager Data Source ::"

    @OptIn(UnstableApi::class)
    @Throws(IllegalStateException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {

        if (!opened) {

            throw IllegalStateException("DataSource not opened")
        }

        if (bytesRemaining == 0L) {

            if (DEBUG.get() && VERBOSE.get()) Console.log("$tag Read :: END")

            return C.RESULT_END_OF_INPUT
        }

        try {

            val bytesRead = inputStream?.read(

                buffer,
                offset,
                length.coerceAtMost(bytesRemaining.toInt())

            ) ?: -1

            if (bytesRead != -1) {

                bytesRemaining -= bytesRead
            }

            if (DEBUG.get() && VERBOSE.get()) Console.log("$tag Read :: $bytesRead")

            return bytesRead

        } catch (e: Throwable) {

            recordException(e)
        }

        return 0
    }

    @OptIn(UnstableApi::class)
    @Throws(IllegalStateException::class)
    override fun open(dataSpec: DataSpec): Long {

        if (opened) {

            throw IllegalStateException("Already opened")
        }

        if (DEBUG.get()) Console.log("$tag Open :: START")

        try {

            val result = runBlocking {

                fetchViaWorkManager(dataSpec.uri.toString())
            }

            this.inputStream = ByteArrayInputStream(result)
            this.bytesRemaining = result.size.toLong()
            this.opened = true

            if (DEBUG.get()) {

                Console.log("$tag Open :: END :: Bytes remaining = $bytesRemaining")
            }

            return bytesRemaining

        } catch (e: Throwable) {

            recordException(e)
        }

        return 0
    }

    @Throws(IOException::class)
    private suspend fun fetchViaWorkManager(url: String): ByteArray {

        if (DEBUG.get()) Console.log("$tag Fetching")

        val context = BaseApplication.Companion.takeContext()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<NetworkWorker>()
            .setInputData(workDataOf("url" to url))
            .setConstraints(constraints)
            .build()

        WorkManager.Companion.getInstance(context).enqueue(workRequest)

        return WorkManager.Companion.getInstance(context)
            .getWorkInfoByIdLiveData(workRequest.id)
            .asFlow()
            .first { workInfo ->
                workInfo?.state?.isFinished == true
            }
            .let { workInfo ->

                if (workInfo?.state == WorkInfo.State.SUCCEEDED) {

                    // FIXME: Do not use filesystem [IN_PROGRESS]
                    readFromCache()

                } else {

                    throw IOException("Work failed: ${workInfo?.state}")
                }
            }
    }

    override fun close() {

        if (DEBUG.get()) Console.log("$tag Close :: START")

        try {

            inputStream?.close()
            opened = false

            if (DEBUG.get()) Console.log("$tag Close :: END")

        } catch (e: Throwable) {

            Console.error(

                "$tag Close :: END :: Error='${e.message ?: e::class.simpleName}'"
            )

            recordException(e)
        }
    }

    override fun getUri(): Uri? = null

    @OptIn(UnstableApi::class)
    override fun addTransferListener(transferListener: TransferListener) {
    }

    override fun getResponseHeaders(): Map<String, List<String>> = emptyMap()

    @Throws(IOException::class)
    private suspend fun readFromCache(): ByteArray {

        if (DEBUG.get()) Console.log("$tag Read from cache")

        val context = BaseApplication.Companion.takeContext()

        val cacheDir = File(context.cacheDir, "media_cache")

        if (!cacheDir.exists()) {

            cacheDir.mkdirs()
        }

        val cacheFile = cacheDir.listFiles()
            ?.filter { it.name.endsWith(".mp3") }
            ?.maxByOrNull { it.lastModified() }
            ?: throw IOException("No cached file found")

        return try {

            cacheFile.inputStream().use { input ->

                ByteArrayOutputStream().use { output ->

                    input.copyTo(output)
                    output.toByteArray()
                }

            }

        } catch (e: Exception) {

            throw IOException("Failed to read cache: ${e.message}", e)
        }
    }

    @SuppressLint("WorkerHasAPublicModifier")
    class NetworkWorker(

        context: Context, params: WorkerParameters

    ) : CoroutineWorker(context, params) {

        private val tag = "Exo :: Worker Manager Data Source :: Network worker ::"

        override suspend fun doWork(): Result {

            if (DEBUG.get()) Console.log("$tag Do work")

            return try {

                val url = inputData.getString("url") ?: return Result.failure()
                val data = fetchMediaFromNetwork(url)

                // FIXME: Do not use filesystem [IN_PROGRESS]
                saveToCache(data, url)

                Result.success(
                    workDataOf(

                        "cache_key" to url.hashCode().toString()
                    )
                )

            } catch (e: Throwable) {

                Result.failure(
                    workDataOf(

                        "error" to e.message
                    )
                )
            }
        }

        @Throws(IOException::class)
        private suspend fun fetchMediaFromNetwork(url: String): ByteArray {

            val tag = "$tag Fetch media from network ::"

            if (DEBUG.get()) Console.log("$tag START")

            val latch = CountDownLatch(1) // FIXME: Do we need this? [IN_PROGRESS]
            var bytes: ByteArray = byteArrayOf()
            val context = BaseApplication.Companion.takeContext()

            context.executeWithWorkManager {

                if (DEBUG.get()) Console.log("$tag Obtaining bytes")

                try {

                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build()

                    val request = Request.Builder()
                        .url(url)
                        .header("Accept", "audio/mpeg")
                        .build()

                    bytes = client.newCall(request).execute().use { response ->

                        if (!response.isSuccessful) {

                            throw IOException("Network error: ${response.code}")
                        }

                        response.body.bytes()
                    }

                    if (DEBUG.get()) Console.log("$tag Bytes obtained")

                    latch.countDown()

                } catch (e: Throwable) {

                    Console.error(

                        "$tag Bytes not obtained :: " +
                                "Error='${e.message ?: e::class.simpleName}'"
                    )

                    recordException(e)

                    latch.countDown()
                }
            }

            try {

                if (DEBUG.get()) Console.log("$tag Await")

                if (latch.await(60, TimeUnit.SECONDS)) {

                    if (DEBUG.get()) Console.log("$tag Await OK")

                    if (bytes.isNotEmpty()) {

                        if (DEBUG.get()) Console.log("$tag END")

                    } else {

                        Console.error("$tag END :: No data received")
                    }

                } else {

                    Console.error("$tag Await timed out")
                }

            } catch (e: Throwable) {

                Console.error(

                    "$tag ERROR :: Latch await error='${e.message ?: e::class.simpleName}'"
                )
            }

            return bytes
        }

        @Throws(IOException::class)
        private suspend fun saveToCache(data: ByteArray, url: String) {

            if (DEBUG.get()) Console.log("$tag Save to cache")

            val context = BaseApplication.Companion.takeContext()

            val cacheDir = File(context.cacheDir, "media_cache")

            if (!cacheDir.exists()) {

                cacheDir.mkdirs()
            }

            val filename = "media_${url.hashCode()}_${System.currentTimeMillis()}.mp3"

            val cacheFile = File(cacheDir, filename)

            try {

                cacheFile.outputStream().use { it.write(data) }

            } catch (e: Exception) {

                cacheFile.delete()

                throw IOException("Failed to write cache: ${e.message}", e)
            }
        }
    }
}