package com.redelf.commons.persistance

class DefaultFacade(builder: PersistenceBuilder) :
    Facade {

    private val storage: Storage?
    private val converter: Converter?
    private val encryption: Encryption?
    private val serializer: Serializer?
    private val logInterceptor: LogInterceptor?

    init {

        storage = builder.storage
        converter = builder.converter
        encryption = builder.encryption
        serializer = builder.serializer
        logInterceptor = builder.logInterceptor

        logInterceptor?.onLog("Data.init -> Encryption : " + encryption?.javaClass?.simpleName)
    }

    override fun <T> put(key: String, value: T): Boolean {
        // Validate
        PersistenceUtils.checkNull("Key", key)
        log("Data.put -> key: " + key + ", value: " + (value != null))

        // If the value is null, delete it
        if (value == null) {
            log("Data.put -> Value is null. Any existing value will be deleted with the given key")
            return delete(key)
        }

        // 1. Convert to text
        val plainText = converter?.toString(value)
        log("Data.put -> Converted: " + (plainText != null))
        if (plainText == null) {
            log("Data.put -> Converter failed")
            return false
        }

        // 2. Encrypt the text
        var cipherText: ByteArray? = null
        try {
            cipherText = encryption?.encrypt(key, plainText)
            log("Data.put -> Encrypted: " + (cipherText != null))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (cipherText == null) {
            log("Data.put -> Encryption failed")
            return false
        }

        // 3. Serialize the given object along with the cipher text
        val serializedText = serializer?.serialize(cipherText, value)
        log("Data.put -> Serialized: " + (serializedText != null))
        if (serializedText == null) {
            log("Data.put -> Serialization failed")
            return false
        }

        // 4. Save to the storage
        return if (storage?.put(key, serializedText) == true) {
            log("Data.put -> Stored successfully")
            true
        } else {
            log("Data.put -> Store operation failed")
            false
        }
    }

    override fun <T> get(key: String): T? {

        log("Data.get -> key: $key")

        // 1. Get serialized text from the storage
        val serializedText = storage?.get<String?>(key)
        log("Data.get -> Fetched from storage: " + (serializedText != null))

        if (serializedText == null) {

            log("Data.get -> Fetching from storage failed")
            return null
        }

        // 2. Deserialize
        val dataInfo = serializer?.deserialize(serializedText)
        log("Data.get -> Deserialized")
        if (dataInfo == null) {

            log("Data.get -> Deserialization failed")
            return null
        }

        // 3. Decrypt
        var plainText: String? = null

        try {

            plainText = encryption?.decrypt(key, dataInfo.cipherText)
            log("Data.get -> Decrypted: " + (plainText != null))

        } catch (e: Exception) {

            log("Data.get -> Decrypt failed: " + e.message)
        }
        if (plainText == null) {
            log("Data.get -> Decrypt failed")
            return null
        }

        // 4. Convert the text to original data along with original type
        var result: T? = null
        try {
            result = converter?.fromString(plainText, dataInfo)
            log("Data.get -> Converted: " + (result != null))
        } catch (e: Exception) {
            val message = e.message
            log("Data.get -> Converter failed, error='$message'")
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
}