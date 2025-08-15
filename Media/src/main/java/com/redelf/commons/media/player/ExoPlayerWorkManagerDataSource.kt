package com.redelf.commons.media.player

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
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
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@UnstableApi
class ExoPlayerWorkManagerDataSource : DataSource {

    private var opened = false
    private var bytesRemaining: Long = 0
    private var inputStream: InputStream? = null

    @OptIn(UnstableApi::class)
    @Throws(IllegalStateException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {

        if (!opened) {

            throw IllegalStateException("DataSource not opened")
        }

        if (bytesRemaining == 0L) {

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

        try {

            val result = runBlocking {

                fetchViaWorkManager(dataSpec.uri.toString())
            }

            this.inputStream = ByteArrayInputStream(result)
            this.bytesRemaining = result.size.toLong()
            this.opened = true

            return bytesRemaining

        } catch (e: Throwable) {

            recordException(e)
        }

        return 0
    }

    private suspend fun fetchViaWorkManager(url: String): ByteArray {

        val context = BaseApplication.takeContext()

        return suspendCoroutine { continuation ->

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()


            val workRequest = OneTimeWorkRequestBuilder<NetworkWorker>()
                .setInputData(workDataOf("url" to url))
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)

            WorkManager.getInstance(context)
                .getWorkInfoByIdLiveData(workRequest.id)
                .observeForever { workInfo ->

                    if (workInfo?.state == WorkInfo.State.SUCCEEDED) {

                        val outputData = workInfo.outputData
                        val byteArray = outputData.getByteArray("data") ?: byteArrayOf()

                        continuation.resume(byteArray)

                    } else if (workInfo?.state == WorkInfo.State.FAILED) {

                        continuation.resumeWithException(

                            IOException("WorkManager failed")
                        )
                    }
                }
        }
    }

    override fun close() {

        try {

            inputStream?.close()
            opened = false

        } catch (e: Throwable) {

            recordException(e)
        }
    }

    override fun getUri(): Uri? = null

    @OptIn(UnstableApi::class)
    override fun addTransferListener(transferListener: TransferListener) {
    }

    override fun getResponseHeaders(): Map<String, List<String>> = emptyMap()

    @SuppressLint("WorkerHasAPublicModifier")
    private class NetworkWorker(

        context: Context, params: WorkerParameters

    ) : CoroutineWorker(context, params) {

        override suspend fun doWork(): Result {

            return try {

                val url = inputData.getString("url")!!
                val data = URL(url).readBytes()

                Result.success(workDataOf("data" to data))

            } catch (e: Throwable) {

                Console.error(e)

                Result.failure()
            }
        }
    }
}