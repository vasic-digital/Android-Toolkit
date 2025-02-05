package com.redelf.commons.test

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.redelf.commons.execution.TaskExecutor
import com.redelf.commons.logging.Console
import com.redelf.commons.persistance.EncryptedPersistence
import org.junit.Assert
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

    protected fun log(what: String) = Console.debug(what)

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

                        Console.log("Skipping the asset: $assetName")
                        return@forEach
                    }
                }

                val outputFile = File(workingDir.absolutePath, assetName)
                val inputStream = testContext.assets.open("$directory/$assetName")

                try {

                    if (outputFile.exists()) {

                        Console.warning("Tmp. file already exists: ${outputFile.absolutePath}")

                        if (outputFile.delete()) {

                            Console.log("Tmp. file deleted: ${outputFile.absolutePath}")

                        } else {

                            val msg = "Tmp. file could not be deleted: ${outputFile.absolutePath}"

                            throw IllegalStateException(msg)
                        }
                    }

                    if (outputFile.createNewFile() && outputFile.exists()) {

                        Console.log("Tmp. file created: ${outputFile.absolutePath}")

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

                    Console.error(e)
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

        keySalt: String? = null,
        storageTag: String? = null

    ): EncryptedPersistence? {

        val instance = instantiatePersistence(

            keySalt = keySalt,
            storageTag = storageTag
        )

        instance?.initialize(ctx)

        return instance
    }

    protected fun instantiatePersistence(

        keySalt: String? = null,
        storageTag: String? = null

    ): EncryptedPersistence? {

        try {

            return EncryptedPersistence(

                doLog = true,
                ctx = applicationContext,
                keySalt = keySalt ?: testSession.toString(),
                storageTag = storageTag ?: testSession.toString()
            )

        } catch (e: Exception) {

            Assert.fail(e.message)
        }

        return null
    }
}