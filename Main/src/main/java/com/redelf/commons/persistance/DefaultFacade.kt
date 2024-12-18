package com.redelf.commons.persistance

import android.content.Context
import com.redelf.commons.extensions.forClassName
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
    private var encryption: Encryption? = null
    private var serializer: Serializer? = null
    private var storage: Storage<String>? = null

    fun initialize(builder: PersistenceBuilder): Facade {

        storage = builder.storage
        converter = builder.converter
        encryption = builder.encryption
        serializer = builder.serializer

        val message = "$TAG Init :: Encryption = " +
                "'${encryption?.javaClass?.canonicalName?.forClassName()}'"

        if (DEBUG.get()) {

            Console.log("$TAG $message")
        }

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

        log("Put :: Key = $key :: Has value = ${value != null}")

        if (value == null) {

            log(

                "Put :: Key = $key :: Null value :: Any existing value will " +
                        "be deleted with the given key"
            )

            return delete(key)
        }

        val plainText = converter?.toString(value)

        log("Put :: Key = $key :: Converted = '$plainText'")

        if (plainText == null) {

            err("Put :: Key = $key :: Converter failed")

            return false
        }

        var cipherText: ByteArray? = null

        try {

            cipherText = encryption?.encrypt(key, plainText)

            log("Put :: Key = $key :: Encrypted :: '$plainText' into '$cipherText'")

        } catch (e: Exception) {

            err("Put :: Key = $key :: Encrypt failed :: Error = '${e.message}'")

            Console.error(e)
        }

        if (cipherText == null) {

            err("Put :: Key = $key :: Encryption failed")

            return false
        }

        val serializedText = serializer?.serialize(cipherText, value)

        log("Put :: Key = $key :: Serialized = '$serializedText'")

        if (serializedText == null) {

            err("Put :: Key = $key :: Serialization failed")

            return false
        }

        return if (storage?.put(key, serializedText) == true) {

            log("Put :: Key = $key :: Stored successfully")

            true

        } else {

            err("Put :: Key = $key :: Store operation failed")

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

            log(" Get :: Key = $key :: Converted: $result")

        } catch (e: Exception) {

            err(" Get :: Key = $key :: Converter failed: ${e.message}")

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

        val tag = " Get :: by type '" + type.typeName + "' :: "

        if (key == null) {

            return null
        }

        val plainText = getRaw(key)

        // 4. Convert the text to original data along with original type
        var result: Any? = null

        try {

            result = converter?.fromString(plainText, type)

            log("$tag Key = $key :: Converted: $result")

        } catch (e: Exception) {

            err("Key :: $key :: Converter failed: ${e.message}")

            Console.error(e)
        }

        return result
    }

    override fun getByClass(key: String?, clazz: Class<*>): Any? {

        val tag = " Get :: by class '" + clazz.simpleName + "' :: "

        if (key == null) {

            return null
        }

        val plainText = getRaw(key)

        // 4. Convert the text to original data along with original type
        var result: Any? = null

        try {

            result = converter?.fromString(plainText, clazz)

            log("$tag Key = $key :: Converted: $result")

        } catch (e: Exception) {

            err("Key :: $key :: Converter failed: ${e.message}")

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

    private fun err(message: String) {

        Console.error("$TAG ERROR: $message")
    }

    private fun getDataInfo(key: String): DataInfo? {

        val tag = " Get :: Data info ::"

        log("$tag Key = $key")

        // 1. Get serialized text from the storage
        val serializedText: String?

        try {

            serializedText = storage?.get(key)

        } catch (e: Exception) {

            err("ERROR: ${e.message}")

            Console.error(e)

            return null
        }

        val empty = isEmpty(serializedText)

        if (empty) {

            log("$tag Key = $key :: Nothing fetched from the storage for Key = $key")

            return null
        }

        log("$tag Key = $key :: Fetched from storage for Key = $key")

        // 2. Deserialize
        return serializer?.deserialize(serializedText)
    }

    private fun getRaw(key: String): String? {

        val tag = " Get :: RAW ::"

        // 2. Deserialize
        val dataInfo = getDataInfo(key)

        if (dataInfo == null) {

            log("$tag Key = $key :: empty info data for Key = $key")

            return null
        }

        log("$tag Key = $key :: Deserialized = '$dataInfo'")

        // 3. Decrypt
        var plainText: String? = null

        try {

            val cText = dataInfo.cipherText
            plainText = encryption?.decrypt(key, cText)

            log("$tag Key = $key :: Decrypted :: '$plainText' from '$cText'")

        } catch (e: Exception) {

            err("$tag Key = $key :: Decrypt failed: ${e.message}")

            Console.error(e)
        }

        if (plainText == null) {

            err("$tag Key = $key :: Decrypt failed")
        }

        return plainText
    }
}