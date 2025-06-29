package com.redelf.commons.persistance

import android.content.Context
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.extensions.forClassName
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.persistance.base.Converter
import com.redelf.commons.persistance.base.Encryption
import com.redelf.commons.persistance.base.Facade
import com.redelf.commons.persistance.base.Serializer
import com.redelf.commons.persistance.base.Storage
import com.redelf.commons.persistance.encryption.NoEncryption
import com.redelf.commons.registration.Registration
import com.redelf.commons.security.encryption.EncryptionListener
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicBoolean

/*
    TODO: Eliminate use of objects (statics) in persistence mechanism
*/
object DefaultFacade : Facade, Registration<EncryptionListener<String, String>> {

    val DEBUG = AtomicBoolean()

    private var converter: Converter? = null
    private var serializer: Serializer? = null
    private var storage: Storage<String>? = null
    private const val TAG = "Facade :: DEFAULT ::"
    private var encryption: Encryption<String>? = null
    private val listeners = Callbacks<EncryptionListener<String, String>>("enc_listeners")

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

    fun isEncryptionEnabled() = encryption !is NoEncryption

    override fun register(subscriber: EncryptionListener<String, String>) {

        if (isRegistered(subscriber)) {

            return
        }

        listeners.register(subscriber)
    }

    override fun unregister(subscriber: EncryptionListener<String, String>) {

        if (isRegistered(subscriber)) {

            listeners.unregister(subscriber)
        }
    }

    override fun isRegistered(subscriber: EncryptionListener<String, String>): Boolean {

        return listeners.isRegistered(subscriber)
    }

    override fun shutdown(): Boolean {

        return storage?.shutdown() == true
    }

    override fun terminate(vararg args: Any): Boolean {

        listeners.clear()

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

        log("Put :: Key = $key :: Converted")

        if (plainText == null) {

            err("Put :: Key = $key :: Converter failed")

            return false
        }

        var cipherText: String? = null

        try {

            cipherText = encryption?.encrypt(key, plainText)

            log("Put :: Key = $key :: Encrypted")

        } catch (e: Throwable) {

            err("Put :: Key = $key :: Encrypt failed :: Error = '${e.message}'")

            Console.error(e)
        }

        if (cipherText == null) {

            err("Put :: Key = $key :: Encryption failed")

            val e = IOException("Encryption failed")
            notifyEncryptedFailed(key, e)

            return false
        }

        notifyEncrypted(key, plainText, cipherText)

        val serializedText = serializer?.serialize(cipherText, value)

        log("Put :: Key = $key :: Serialized")

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

    override fun <T> get(key: String?, callback: OnObtain<T?>) {

        if (key == null) {

            val e = IllegalArgumentException("Key is null")
            callback.onFailure(e)
            return
        }

        getDataInfo(

            key,

            object : OnObtain<DataInfo?> {

                override fun onCompleted(data: DataInfo?) {

                    val dataInfo = data

                    getRaw(

                        key,

                        object : OnObtain<String?> {

                            override fun onCompleted(data: String?) {

                                val plainText = data

                                try {

                                    val result: T? = converter?.fromString(plainText, dataInfo)

                                    log(" Get :: Key = $key :: Converted: $result")

                                    callback.onCompleted(result)

                                } catch (e: Throwable) {

                                    callback.onFailure(e)
                                }
                            }

                            override fun onFailure(error: Throwable) {

                                callback.onFailure(error)
                            }
                        }
                    )
                }

                override fun onFailure(error: Throwable) {

                    callback.onFailure(error)
                }
            }
        )
    }

    override fun <T> get(key: String?, defaultValue: T, callback: OnObtain<T?>) {

        get<T>(

            key,

            object : OnObtain<T?> {

                override fun onCompleted(data: T?) {

                    data?.let {

                        callback.onCompleted(it)
                    }

                    if (data == null) {

                        callback.onCompleted(defaultValue)
                    }
                }

                override fun onFailure(error: Throwable) {

                    callback.onFailure(error)
                }
            }
        )
    }

    override fun getByType(key: String?, type: Type, callback: OnObtain<Any?>) {

        val tag = " Get :: by type '" + type.typeName + "' :: "

        if (key == null) {

            val e = IllegalArgumentException("Key is null")
            callback.onFailure(e)
            return
        }

        getRaw(

            key,

            object : OnObtain<String?> {

                override fun onCompleted(data: String?) {

                    val plainText = data

                    try {

                        val result: Any? = converter?.fromString(plainText, type)

                        log("$tag Key = $key :: Converted")

                        callback.onCompleted(result)

                    } catch (e: Throwable) {

                        callback.onFailure(e)
                    }
                }

                override fun onFailure(error: Throwable) {

                    callback.onFailure(error)
                }
            }
        )
    }

    override fun getByClass(key: String?, clazz: Class<*>, callback: OnObtain<Any?>) {

        val tag = " Get :: by class '" + clazz.simpleName + "' :: "

        if (key == null) {

            val e = IllegalArgumentException("Key is null")
            callback.onFailure(e)
            return
        }

        getRaw(

            key,

            object : OnObtain<String?> {

                override fun onCompleted(data: String?) {

                    val plainText = data

                    try {

                        val result: Any? = converter?.fromString(plainText, clazz)

                        log("$tag Key = $key :: Converted")

                        callback.onCompleted(result)

                    } catch (e: Throwable) {

                        callback.onFailure(e)
                    }
                }

                override fun onFailure(error: Throwable) {

                    callback.onFailure(error)
                }
            }
        )
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

    override fun contains(key: String?, callback: OnObtain<Boolean>) {

        storage?.contains(key, callback)
    }

    override fun destroy() {

        listeners.clear()
    }

    private fun log(message: String) {

        if (DEBUG.get()) {

            Console.log("$TAG $message")
        }
    }

    private fun err(message: String) {

        Console.error("$TAG ERROR: $message")
    }

    private fun getDataInfo(key: String, callback: OnObtain<DataInfo?>) {

        val tag = " Get :: Data info ::"

        log("$tag Key = $key")

        try {

            storage?.get(

                key,

                object : OnObtain<String?> {

                    override fun onCompleted(data: String?) {

                        val serializedText = data
                        val empty = isEmpty(serializedText)

                        if (empty) {

                            log("$tag Key = $key :: Nothing fetched from the storage for Key = $key")

                            callback.onCompleted(null)

                        } else {

                            log("$tag Key = $key :: Fetched from storage for Key = $key")

                            val info = serializer?.deserialize(serializedText)
                            callback.onCompleted(info)
                        }
                    }

                    override fun onFailure(error: Throwable) {

                        callback.onFailure(error)
                    }
                }
            )

        } catch (e: Throwable) {

            callback.onFailure(e)
            return
        }
    }

    private fun getRaw(key: String, callback: OnObtain<String?>) {

        val tag = " Get :: RAW ::"

        getDataInfo(

            key,

            object : OnObtain<DataInfo?> {

                override fun onCompleted(data: DataInfo?) {

                    val dataInfo = data

                    if (dataInfo == null) {

                        log("$tag Key = $key :: empty info data for Key = $key")

                        callback.onCompleted(null)
                        return
                    }

                    log("$tag Key = $key :: Deserialized")

                    try {

                        val cText = dataInfo.cipherText ?: ""

                        if (isEmpty(cText)) {

                            log("$tag Key = $key :: Decrypted :: Got empty")

                            notifyDecrypted(key, "", "")

                            callback.onCompleted("")

                        } else {

                            val plainText = encryption?.decrypt(key, cText)

                            log("$tag Key = $key :: Decrypted")

                            notifyDecrypted(key, cText, plainText ?: "")

                            callback.onCompleted(plainText)
                        }

                    } catch (e: Throwable) {

                        err("$tag Key = $key :: Decrypt failed: ${e.message}")

                        callback.onFailure(e)

                        notifyDecryptedFailed(key, e)
                    }
                }

                override fun onFailure(error: Throwable) {

                    callback.onFailure(error)
                }
            }
        )
    }

    private fun notifyEncrypted(key: String, raw: String, encrypted: String) {

        listeners.doOnAll(

            object : CallbackOperation<EncryptionListener<String, String>> {

                override fun perform(callback: EncryptionListener<String, String>) {

                    callback.onEncrypted(key, raw, encrypted)
                }
            },

            "encrypted.$key"
        )
    }

    private fun notifyDecrypted(key: String, encrypted: String, decrypted: String) {

        listeners.doOnAll(

            object : CallbackOperation<EncryptionListener<String, String>> {

                override fun perform(callback: EncryptionListener<String, String>) {

                    callback.onDecrypted(key, encrypted, decrypted)
                }
            },

            "decrypted.$key"
        )
    }

    private fun notifyEncryptedFailed(key: String, error: Throwable) {

        listeners.doOnAll(

            object : CallbackOperation<EncryptionListener<String, String>> {

                override fun perform(callback: EncryptionListener<String, String>) {

                    callback.onEncryptionFailure(key, error)
                }
            },

            "encryption.failure"
        )
    }

    private fun notifyDecryptedFailed(key: String, error: Throwable) {

        listeners.doOnAll(

            object : CallbackOperation<EncryptionListener<String, String>> {

                override fun perform(callback: EncryptionListener<String, String>) {

                    callback.onDecryptionFailure(key, error)
                }
            },

            "decryption.failure"
        )
    }
}