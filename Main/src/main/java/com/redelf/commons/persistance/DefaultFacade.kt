package com.redelf.commons.persistance

import android.content.Context
import com.redelf.commons.isEmpty
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

object DefaultFacade : Facade {

    private val doLog = AtomicBoolean()
    private var converter: Converter? = null
    private var encryption: Encryption? = null
    private var serializer: Serializer? = null
    private var storage: Storage<String>? = null
    private const val LOG_TAG = "Default facade ::"
    private var logInterceptor: LogInterceptor? = null

    fun initialize(builder: PersistenceBuilder): Facade {

        storage = builder.storage
        converter = builder.converter
        encryption = builder.encryption
        serializer = builder.serializer
        logInterceptor = builder.logInterceptor

        doLog.set(builder.doLog)

        log("init -> Encryption : " + encryption?.javaClass?.simpleName)

        return this
    }

    override fun shutdown(): Boolean {

        return storage?.shutdown() ?: false
    }

    override fun initialize(ctx: Context) {

        storage?.initialize(ctx)
    }

    override fun <T> put(key: String, value: T): Boolean {

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

            err("put -> Converter failed")
            return false
        }

        // 2. Encrypt the text
        var cipherText: ByteArray? = null

        try {

            cipherText = encryption?.encrypt(key, plainText)
            log("put -> Encrypted: " + (cipherText != null))

        } catch (e: Exception) {

            Timber.e(e)
        }

        if (cipherText == null) {

            err("put -> Encryption failed")
            return false
        }

        // 3. Serialize the given object along with the cipher text
        val serializedText = serializer?.serialize(cipherText, value)

        log("put -> Serialized: " + (serializedText != null))

        if (serializedText == null) {

            err("put -> Serialization failed")
            return false
        }

        // 4. Save to the storage
        return if (storage?.put(key, serializedText) == true) {

            log("put -> Stored successfully")
            true

        } else {

            err("put -> Store operation failed")
            false
        }
    }

    override fun <T> get(key: String): T? {

        log("get -> key: $key")

        // 1. Get serialized text from the storage
        val serializedText: String?

        try {

            serializedText = storage?.get(key)

        } catch (e: Exception) {

            Timber.e(LOG_TAG, e)

            return null
        }

        val empty = isEmpty(serializedText)

        if (empty) {

            log("get -> Nothing fetched from the storage for key: $key")
            return null
        }

        log("get -> Fetched from storage for key: $key")

        // 2. Deserialize
        val dataInfo = serializer?.deserialize(serializedText)

        if (dataInfo == null) {

            err("get -> Deserialization failed for key: $key")
            return null
        }

        log("get -> Deserialized")

        // 3. Decrypt
        var plainText: String? = null

        try {

            plainText = encryption?.decrypt(key, dataInfo.cipherText)
            log("get -> Decrypted: " + (plainText != null))

        } catch (e: Exception) {

            err("get -> Decrypt failed: " + e.message)
        }

        if (plainText == null) {

            err("get -> Decrypt failed")
            return null
        }

        // 4. Convert the text to original data along with original type
        var result: T? = null
        try {

            result = converter?.fromString(plainText, dataInfo)
            log("get -> Converted: " + (result != null))

        } catch (e: Exception) {

            val message = e.message
            err("get -> Converter failed, error='$message'")
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

    override fun contains(key: String): Boolean {

        return storage?.contains(key) ?: false
    }

    override fun isBuilt(): Boolean {
        return true
    }

    override fun destroy() {}
    private fun log(message: String) {

        if (doLog.get()) {

            logInterceptor?.onLog("$LOG_TAG $message")
        }
    }

    private fun err(message: String) {

        logInterceptor?.onError("$LOG_TAG $message")
    }
}