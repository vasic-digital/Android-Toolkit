package com.redelf.commons.persistance

import android.content.Context
import com.redelf.commons.isEmpty
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

object DefaultFacade : Facade {

    private val doLog = AtomicBoolean()
    private val logRawData = AtomicBoolean()
    private val keysFilter = CopyOnWriteArrayList<String>()

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
        logRawData.set(builder.logRawData)
        keysFilter.addAll(builder.keysFilter)

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

        log("put -> key: $key -> has value: ${value != null}")

        // If the value is null, delete it
        if (value == null) {

            log("put -> key: $key -> null value, any existing value will be deleted with the given key")
            return delete(key)
        }

        // 1. Convert to text
        val plainText = converter?.toString(value)

        var logValue = "${plainText != null}"
        if (canLogKey(key)) {

            logValue = plainText.toString()
        }

        if (canLogKey(key)) {

            dbg("put -> key: $key -> Raw: $logValue")

        } else {

            log("put -> key: $key -> Converted: $logValue")
        }

        if (plainText == null) {

            err("put -> key: $key -> Converter failed")
            return false
        }

        // 2. Encrypt the text
        var cipherText: ByteArray? = null

        try {

            cipherText = encryption?.encrypt(key, plainText)
            log("put -> key: $key -> Encrypted: " + (cipherText != null))

        } catch (e: Exception) {

            Timber.e(e)
        }

        if (cipherText == null) {

            err("put -> key: $key -> Encryption failed")
            return false
        }

        // 3. Serialize the given object along with the cipher text
        val serializedText = serializer?.serialize(cipherText, value)

        log("put -> key: $key -> Serialized: " + (serializedText != null))

        if (serializedText == null) {

            err("put -> key: $key -> Serialization failed")
            return false
        }

        // 4. Save to the storage
        return if (storage?.put(key, serializedText) == true) {

            log("put -> key: $key -> Stored successfully")
            true

        } else {

            err("put -> key: $key -> Store operation failed")
            false
        }
    }

    override fun <T> get(key: String): T? {

        log("get -> key: $key -> key: $key")

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

            log("get -> key: $key -> Nothing fetched from the storage for key: $key")
            return null
        }

        log("get -> key: $key -> Fetched from storage for key: $key")

        // 2. Deserialize
        val dataInfo = serializer?.deserialize(serializedText)

        if (dataInfo == null) {

            err("get -> key: $key -> Deserialization failed for key: $key")
            return null
        }

        log("get -> key: $key -> Deserialized")

        // 3. Decrypt
        var plainText: String? = null

        try {

            plainText = encryption?.decrypt(key, dataInfo.cipherText)

            var logValue = "${plainText != null}"
            if (canLogKey(key)) {

                logValue = plainText.toString()
            }

            if (canLogKey(key)) {

                dbg("get -> key: $key -> Decrypted: $logValue")

            } else {

                log("get -> key: $key -> Decrypted: $logValue")
            }

        } catch (e: Exception) {

            err("get -> key: $key -> Decrypt failed: " + e.message)
        }

        if (plainText == null) {

            err("get -> key: $key -> Decrypt failed")
            return null
        }

        // 4. Convert the text to original data along with original type
        var result: T? = null
        try {

            result = converter?.fromString(plainText, dataInfo)

            var logValue = "${result != null}"
            if (canLogKey(key)) {

                logValue = result.toString()
            }

            if (canLogKey(key)) {

                dbg("get -> key: $key -> Converted: $logValue")

            } else {

                log("get -> key: $key -> Converted: $logValue")
            }

        } catch (e: Exception) {

            val message = e.message
            err("get -> key: $key -> Converter failed, error='$message'")
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

    private fun dbg(message: String) {

        if (doLog.get()) {

            logInterceptor?.onDebug("$LOG_TAG $message")
        }
    }

    private fun err(message: String) {

        logInterceptor?.onError("$LOG_TAG $message")
    }

    private fun canLogKey(key: String): Boolean {

        return logRawData.get() && (keysFilter.isEmpty() || keysFilter.contains(key))
    }
}