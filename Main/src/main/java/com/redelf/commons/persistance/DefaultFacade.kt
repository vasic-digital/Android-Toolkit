package com.redelf.commons.persistance

import android.content.Context
import com.redelf.commons.compress
import com.redelf.commons.decompress
import timber.log.Timber

object DefaultFacade : Facade {

    private var converter: Converter? = null
    private var encryption: Encryption? = null
    private var serializer: Serializer? = null
    private var storage: Storage<String>? = null
    private var logInterceptor: LogInterceptor? = null

    fun initialize(builder: PersistenceBuilder): Facade {

        storage = builder.storage
        converter = builder.converter
        encryption = builder.encryption
        serializer = builder.serializer
        logInterceptor = builder.logInterceptor

        logInterceptor?.onLog("init -> Encryption : " + encryption?.javaClass?.simpleName)

        return this
    }

    override fun shutdown(): Boolean {

        return storage?.shutdown() ?: false
    }

    override fun initialize(ctx: Context) {

        storage?.initialize(ctx)
    }

    override fun <T> put(key: String, value: T): Boolean {

        val doLog = false

        // Validate
        PersistenceUtils.checkNull("Key", key)
        if (doLog) log("put -> key: " + key + ", value: " + (value != null))

        // If the value is null, delete it
        if (value == null) {

            log("put -> Value is null. Any existing value will be deleted with the given key")
            return delete(key)
        }

        // 1. Convert to text
        val plainText = converter?.toString(value)
        if (doLog) log("put -> Converted: " + (plainText != null))

        if (plainText == null) {

            log("put -> Converter failed")
            return false
        }

        // 2. Encrypt the text
        var cipherText: ByteArray? = null

        try {

            cipherText = encryption?.encrypt(key, plainText)
            if (doLog) log("put -> Encrypted: " + (cipherText != null))

        } catch (e: Exception) {

            Timber.e(e)
        }

        if (cipherText == null) {

            log("put -> Encryption failed")
            return false
        }

        // 3. Serialize the given object along with the cipher text
        val serializedText = serializer?.serialize(cipherText, value)

        if (doLog) log("put -> Serialized: " + (serializedText != null))

        if (serializedText == null) {

            log("put -> Serialization failed")
            return false
        }

        // 4. Save to the storage
        return if (storage?.put(key, serializedText) == true) {

            if (doLog) log("put -> Stored successfully")
            true

        } else {

            log("put -> Store operation failed")
            false
        }
    }

    override fun <T> get(key: String): T? {

        val doLog = false

        if (doLog) log("get -> key: $key")

        // 1. Get serialized text from the storage
        val serializedText: String?

        try {

            serializedText = storage?.get(key)

        } catch (e: Exception) {

            Timber.e(e)

            return null
        }

        if (doLog) log("get -> Fetched from storage: " + (serializedText != null))

        if (serializedText == null) {

            log("get -> Fetching from storage failed")
            return null
        }

        // 2. Deserialize
        val dataInfo = serializer?.deserialize(serializedText)
        if (doLog) log("get -> Deserialized")

        if (dataInfo == null) {

            log("get -> Deserialization failed")
            return null
        }

        // 3. Decrypt
        var plainText: String? = null

        try {

            plainText = encryption?.decrypt(key, dataInfo.cipherText)
            if (doLog) log("get -> Decrypted: " + (plainText != null))

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
            if (doLog) log("get -> Converted: " + (result != null))

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
}