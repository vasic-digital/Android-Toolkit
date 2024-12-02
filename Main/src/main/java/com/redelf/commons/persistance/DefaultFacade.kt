package com.redelf.commons.persistance

import android.content.Context
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.logging.Console
import com.redelf.commons.persistance.base.Converter
import com.redelf.commons.persistance.base.Encryption
import com.redelf.commons.persistance.base.Facade
import com.redelf.commons.persistance.base.Serializer
import com.redelf.commons.persistance.base.Storage
import java.lang.reflect.Type
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/*
    TODO: Eliminate use of objects (statics) in persistence mechanism
*/
object DefaultFacade : Facade {

    val DEBUG = AtomicBoolean()

    private val TAG = "Facade :: DEFAULT ::"
    private var converter: Converter? = null
    private val logRawData = AtomicBoolean()
    private var encryption: Encryption? = null
    private var serializer: Serializer? = null
    private var storage: Storage<String>? = null
    private val keysFilter = CopyOnWriteArrayList<String>()

    fun initialize(builder: PersistenceBuilder): Facade {

        storage = builder.storage
        converter = builder.converter
        encryption = builder.encryption
        serializer = builder.serializer

        logRawData.set(builder.logRawData)
        keysFilter.addAll(builder.keysFilter)

        val message =

            "init -> encryption='${encryption?.javaClass?.simpleName}', " +
                "logRawData=${logRawData.get()}, " +
                    "keysFilter=${keysFilter.toMutableList()}"

        if (DEBUG.get()) Console.log("$TAG $message")

        return this
    }

    override fun shutdown(): Boolean {

        return storage?.shutdown() == true
    }

    override fun terminate(vararg args: Any): Boolean {

        return storage?.terminate(*args) == true
    }

    override fun initialize(ctx: Context) {

        storage?.initialize(ctx)
    }

    override fun <T> put(key: String?, value: T): Boolean {

        if (key == null) {

            return false
        }

        if (canLogKey(key)) log("put -> key: $key -> has value: ${value != null}")

        if (value == null) {

            if (canLogKey(key)) log(

                "put -> key: $key -> null value, any existing value will " +
                        "be deleted with the given key"
            )

            return delete(key)
        }

        val plainText = converter?.toString(value)

        if (canLogKey(key)) {

            dbg("put -> key: $key -> Raw: $plainText")

        } else {

            if (canLogKey(key)) log("put -> key: $key -> Converted: ${plainText != null}")
        }

        if (plainText == null) {

            err("put -> key: $key -> Converter failed")
            return false
        }

        var cipherText: ByteArray? = null

        try {

            cipherText = encryption?.encrypt(key, plainText)

            if (canLogKey(key)) log("put -> key: $key -> Encrypted: " + (cipherText != null))

        } catch (e: Exception) {

            Console.error("$TAG put -> key: $key -> Encrypt failed: ${e.message}")
            Console.error(e)
        }

        if (cipherText == null) {

            err("put -> key: $key -> Encryption failed")
            return false
        }

        val serializedText = serializer?.serialize(cipherText, value)

        if (canLogKey(key)) log("put -> key: $key -> Serialized: " + (serializedText != null))

        if (serializedText == null) {

            err("put -> key: $key -> Serialization failed")

            return false
        }

        return if (storage?.put(key, serializedText) == true) {

            if (canLogKey(key)) log("put -> key: $key -> Stored successfully")

            true

        } else {

            err("put -> key: $key -> Store operation failed")

            false
        }
    }

    override fun <T> get(key: String?): T? {

        if (key == null) {

            return null
        }

        val dataInfo = getDataInfo(key)
        val plainText = getRaw(key)

        // 4. Convert the text to original data along with original type
        var result: T? = null

        try {

            result = converter?.fromString(plainText, dataInfo)

            if (canLogKeyRaw(key)) {

                dbg("get -> key: $key -> Converted: $result")

            } else {

                if (canLogKey(key)) log("get -> key: $key -> Converted: ${result != null}")
            }

        } catch (e: Exception) {

            Console.error("$TAG get -> key: $key -> Converter failed: ${e.message}")
            Console.error(e)
        }

        return result
    }

    override fun <T> get(key: String?, defaultValue: T): T {

        if (key == null) {

            return defaultValue
        }

        return get<T>(key) ?: return defaultValue
    }

    override fun getByType(key: String?, type: Type): Any? {

        val tag = "get -> by type '" + type.typeName + "' -> "

        if (key == null) {

            return null
        }

        val plainText = getRaw(key)

        // 4. Convert the text to original data along with original type
        var result: Any? = null

        try {

            result = converter?.fromString(plainText, type)

            if (canLogKeyRaw(key)) {

                dbg("$tag key: $key -> Converted: $result")

            } else {

                if (canLogKey(key)) log("$tag key: $key -> Converted: ${result != null}")
            }

        } catch (e: Exception) {

            Console.error("$TAG key: $key -> Converter failed: ${e.message}")
            Console.error(e)
        }

        return result
    }

    override fun getByClass(key: String?, clazz: Class<*>): Any? {

        val tag = "get -> by class '" + clazz.simpleName + "' -> "

        if (key == null) {

            return null
        }

        val plainText = getRaw(key)

        // 4. Convert the text to original data along with original type
        var result: Any? = null

        try {

            result = converter?.fromString(plainText, clazz)

            if (canLogKeyRaw(key)) {

                dbg("$tag key: $key -> Converted: $result")

            } else {

                if (canLogKey(key)) log("$tag key: $key -> Converted: ${result != null}")
            }

        } catch (e: Exception) {

            Console.error("$TAG key: $key -> Converter failed: ${e.message}")
            Console.error(e)
        }

        return result
    }

    override fun count(): Long {

        return storage?.count() ?: -1
    }

    override fun deleteAll(): Boolean {

        return storage?.deleteAll() == true
    }

    override fun delete(key: String?): Boolean {

        return storage?.delete(key) == true
    }

    override fun contains(key: String?): Boolean {

        return storage?.contains(key) == true
    }

    override fun destroy() = Unit

    private fun log(message: String) {

        if (DEBUG.get()) {

            Console.log("$TAG $message")
        }
    }

    private fun dbg(message: String) {

        if (DEBUG.get()) {

            Console.debug("$TAG $message")
        }
    }

    private fun err(message: String) {

        Console.error("$TAG ERROR: $message")
    }

    private fun canLogKeyRaw(key: String): Boolean {

        return DEBUG.get() && logRawData.get() && (keysFilter.isEmpty() || keysFilter.contains(key))
    }


    private fun canLogKey(key: String): Boolean {

        return DEBUG.get() && (keysFilter.isEmpty() || keysFilter.contains(key))
    }

    private fun getDataInfo(key: String): DataInfo? {

        val tag = "get -> data info ->"

        if (canLogKey(key)) log("$tag key: $key -> key: $key")

        // 1. Get serialized text from the storage
        val serializedText: String?

        try {

            serializedText = storage?.get(key)

        } catch (e: Exception) {

            Console.error("$TAG ERROR: ${e.message}")
            Console.error(e)

            return null
        }

        val empty = isEmpty(serializedText)

        if (empty) {

            if (canLogKey(key)) log("$tag key: $key -> Nothing fetched from the storage for key: $key")

            return null
        }

        if (canLogKey(key)) log("$tag key: $key -> Fetched from storage for key: $key")

        // 2. Deserialize
        return serializer?.deserialize(serializedText)
    }

    private fun getRaw(key: String): String? {

        val tag = "get -> raw ->"

        // 2. Deserialize
        val dataInfo = getDataInfo(key)

        if (dataInfo == null) {

            dbg("$tag key: $key -> empty info data for key: $key")

            return null
        }

        if (canLogKey(key)) log("$tag key: $key -> Deserialized")

        // 3. Decrypt
        var plainText: String? = null

        try {

            plainText = encryption?.decrypt(key, dataInfo.cipherText)

            if (canLogKeyRaw(key)) {

                dbg("$tag key: $key -> Decrypted: $plainText")

            } else {

                if (canLogKey(key)) log("$tag key: $key -> Decrypted: ${plainText != null}")
            }

        } catch (e: Exception) {

            Console.error("$TAG key: $key -> Decrypt failed: ${e.message}")
            Console.error(e)
        }

        if (plainText == null) {

            err("$tag key: $key -> Decrypt failed")
        }

        return plainText
    }
}