package com.redelf.commons.persistance

import com.redelf.commons.exec
import com.redelf.commons.execution.Execution
import com.redelf.commons.execution.TaskExecutor
import com.redelf.commons.recordException
import timber.log.Timber
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor

object DefaultFacade : Facade {

    private var storage: Storage? = null
    private var converter: Converter? = null
    private var encryption: Encryption? = null
    private var serializer: Serializer? = null
    private var logInterceptor: LogInterceptor? = null
    private var executor = TaskExecutor.instantiateSingle()

    fun initialize(builder: PersistenceBuilder, executorToSet: ThreadPoolExecutor? = null) {

        storage = builder.storage
        converter = builder.converter
        encryption = builder.encryption
        serializer = builder.serializer
        logInterceptor = builder.logInterceptor

        executorToSet?.let {

            executor = executorToSet
        }

        logInterceptor?.onLog("init -> Encryption : " + encryption?.javaClass?.simpleName)
    }



    override fun <T> put(key: String, value: T): Boolean {

        val callable = object : Callable<Boolean> {

            override fun call(): Boolean {

                // Validate
                PersistenceUtils.checkNull("Key", key)
                log("put -> key: " + key + ", value: " + (value != null))

                // If the value is null, delete it
                if (value == null) {
                    log("put -> Value is null. Any existing value will be deleted with the given key")
                    return delete(key)
                }

                // 1. Convert to text
                val plainText = converter?.toString(value)
                log("put -> Converted: " + (plainText != null))
                if (plainText == null) {
                    log("put -> Converter failed")
                    return false
                }

                // 2. Encrypt the text
                var cipherText: ByteArray? = null
                try {
                    cipherText = encryption?.encrypt(key, plainText)
                    log("put -> Encrypted: " + (cipherText != null))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (cipherText == null) {
                    log("put -> Encryption failed")
                    return false
                }

                // 3. Serialize the given object along with the cipher text
                val serializedText = serializer?.serialize(cipherText, value)
                log("put -> Serialized: " + (serializedText != null))
                if (serializedText == null) {
                    log("put -> Serialization failed")
                    return false
                }

                // 4. Save to the storage
                return if (storage?.put(key, serializedText) == true) {

                    log("put -> Stored successfully")
                    true

                } else {

                    log("put -> Store operation failed")
                    false
                }
            }
        }

        return exec(callable, executor = getExecutor())
    }

    override fun <T> get(key: String): T? {

        log("get -> key: $key")

        // 1. Get serialized text from the storage
        val serializedText = storage?.get<String?>(key)
        log("get -> Fetched from storage: " + (serializedText != null))

        if (serializedText == null) {

            log("get -> Fetching from storage failed")
            return null
        }

        // 2. Deserialize
        val dataInfo = serializer?.deserialize(serializedText)
        log("get -> Deserialized")
        if (dataInfo == null) {

            log("get -> Deserialization failed")
            return null
        }

        // 3. Decrypt
        var plainText: String? = null

        try {

            plainText = encryption?.decrypt(key, dataInfo.cipherText)
            log("get -> Decrypted: " + (plainText != null))

        } catch (e: Exception) {

            log("get -> Decrypt failed: " + e.message)
        }
        if (plainText == null) {
            log("get -> Decrypt failed")
            return null
        }

        // 4. Convert the text to original data along with original type
        var result: T? = null
        try {
            result = converter?.fromString(plainText, dataInfo)
            log("get -> Converted: " + (result != null))
        } catch (e: Exception) {
            val message = e.message
            log("get -> Converter failed, error='$message'")
        }
        return result
    }

    override fun <T> get(key: String, defaultValue: T): T {

        return get<T>(key) ?: return defaultValue
    }

    override fun count(): Long {

        return storage?.count() ?: -1
    }

    override fun deleteAll(): Boolean {

        return storage?.deleteAll() ?: false
    }

    override fun delete(key: String): Boolean {

        return storage?.delete(key) ?: false
    }

    override fun deleteKeysWithPrefix(value: String?): Boolean {

        return storage?.deleteKeysWithPrefix(value) ?: true
    }

    override fun contains(key: String): Boolean {

        return storage?.contains(key) ?: false
    }

    override fun isBuilt(): Boolean {
        return true
    }

    override fun destroy() {}
    private fun log(message: String) {

        logInterceptor?.onLog(message)
    }

    private fun getExecutor() = object : Execution {

        @Throws(RejectedExecutionException::class)
        override fun <T> execute(callable: Callable<T>): Future<T> {

            return executor.submit(callable)
        }

        override fun execute(action: Runnable, delayInMillis: Long) {

            executor.execute {

                try {

                    Thread.sleep(delayInMillis)

                    action.run()

                } catch (e: InterruptedException) {

                    Timber.e(e)
                }
            }
        }

        override fun execute(what: Runnable) {

            try {

                executor.execute(what)

            } catch (e: RejectedExecutionException) {

                recordException(e)
            }
        }
    }
}