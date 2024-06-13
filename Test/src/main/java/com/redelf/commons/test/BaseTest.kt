package com.redelf.commons.test

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.redelf.commons.execution.TaskExecutor
import com.redelf.commons.logging.Timber
import com.redelf.commons.management.DataManagement
import com.redelf.commons.persistance.EncryptedPersistence
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

abstract class BaseTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    protected val testEnd = "TEST END"
    protected val testStart = "TEST START"
    protected val testPrepare = "TEST PREPARE"
    protected val testSession = UUID.randomUUID()
    protected val executor = TaskExecutor.instantiate(5)
    protected val testContext: Context = instrumentation.context
    protected val applicationContext = instrumentation.targetContext

    protected fun log(what: String) = Timber.d(what)

    @Throws(IllegalStateException::class, IOException::class)
    protected fun uploadAssets(

        directory: String,
        assetsToInclude: List<String>? = null

    ): List<File> {

        val context = applicationContext
        val workingDir = context.cacheDir
        val eMsg = "No test assets available for the directory: $directory"
        val exception = IllegalStateException(eMsg)

        testContext.assets.list(directory)?.let {

            if (it.isEmpty()) {
                throw exception
            }
            val assets = mutableListOf<File>()
            it.forEach { assetName ->

                assetsToInclude?.let { toInclude ->

                    if (!toInclude.contains(assetName)) {

                        Timber.v("Skipping the asset: $assetName")
                        return@forEach
                    }
                }

                val outputFile = File(workingDir.absolutePath, assetName)
                val inputStream = testContext.assets.open("$directory/$assetName")

                try {

                    if (outputFile.exists()) {

                        Timber.w("Tmp. file already exists: ${outputFile.absolutePath}")

                        if (outputFile.delete()) {

                            Timber.v("Tmp. file deleted: ${outputFile.absolutePath}")

                        } else {

                            val msg = "Tmp. file could not be deleted: ${outputFile.absolutePath}"

                            throw IllegalStateException(msg)
                        }
                    }

                    if (outputFile.createNewFile() && outputFile.exists()) {

                        Timber.v("Tmp. file created: ${outputFile.absolutePath}")

                    } else {

                        val msg = "Tmp. file could not be created: ${outputFile.absolutePath}"

                        throw IllegalStateException(msg)
                    }

                    val outputStream = FileOutputStream(outputFile)
                    val bufferedOutputStream = BufferedOutputStream(outputStream)

                    inputStream.copyTo(bufferedOutputStream, 4096)

                    bufferedOutputStream.close()
                    outputStream.close()

                } catch (e: IOException) {

                    Timber.e(e)
                }

                inputStream.close()

                if (!outputFile.exists() || outputFile.length() == 0L) {

                    throw IllegalStateException("Couldn't upload asset: $assetName")
                }

                assets.add(outputFile)
            }

            return assets
        }

        throw exception
    }

    protected fun instantiatePersistenceAndInitialize(

        ctx: Context = applicationContext,
        doEncrypt: Boolean = true,

        keySalt: String? = null,
        storageTag: String? = null

    ): EncryptedPersistence {

        val instance = instantiatePersistence(

            doEncrypt = doEncrypt,
            keySalt = keySalt,
            storageTag = storageTag
        )

        instance.initialize(ctx)

        return instance
    }

    protected fun instantiatePersistence(

        doEncrypt: Boolean = true,

        keySalt: String? = null,
        storageTag: String? = null

    ): EncryptedPersistence {

        return EncryptedPersistence(

            ctx = applicationContext,
            doLog = true,
            logRawData = true,
            doEncrypt = doEncrypt,
            keySalt = keySalt ?: testSession.toString(),
            storageTag = storageTag ?: testSession.toString(),
            logStorageKeys = DataManagement.LOGGABLE_STORAGE_KEYS
        )
    }
}