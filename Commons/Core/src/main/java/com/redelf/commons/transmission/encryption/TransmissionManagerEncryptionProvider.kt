package com.redelf.commons.transmission.encryption

import android.text.TextUtils
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.persistance.Data
import com.redelf.commons.recordException
import com.redelf.commons.security.encryption.AES
import com.redelf.commons.security.encryption.Encryption
import com.redelf.commons.security.encryption.EncryptionProvider
import com.redelf.commons.security.encryption.salt.SaltProvider
import com.redelf.commons.transmission.TransmissionManager
import timber.log.Timber
import java.util.*

class TransmissionManagerEncryptionProvider(

    private val manager: TransmissionManager<*>,
    encryptionKeySuffix: String,
    private val saltProvider: SaltProvider<String> = DefaultSaltProvider(encryptionKeySuffix)

) : EncryptionProvider {

    private var encryptionKey = "KEY_ENCRYPTION_IDENTIFIER_$encryptionKeySuffix"

    private val callbacks =
        Callbacks<TransmissionManagerEncryptionProviderCallback>(

            identifier = "Transmission encryption provider"
        )

    private val callback = object : TransmissionManagerEncryptionProviderCallback {

        override fun onNewEncryptionKeyGenerated(key: String, success: Boolean) {

            callbacks.doOnAll(
                object : CallbackOperation<TransmissionManagerEncryptionProviderCallback> {
                    override fun perform(callback: TransmissionManagerEncryptionProviderCallback) {

                        callback.onNewEncryptionKeyGenerated(key, success)
                    }
                }
            )
        }

        override fun onExistingEncryptionKeyObtained(key: String, success: Boolean) {

            callbacks.doOnAll(
                object : CallbackOperation<TransmissionManagerEncryptionProviderCallback> {
                    override fun perform(callback: TransmissionManagerEncryptionProviderCallback) {

                        callback.onExistingEncryptionKeyObtained(key, success)
                    }
                }
            )
        }
    }

    @Throws(IllegalStateException::class)
    override fun obtain(): Encryption<String, String> {

        try {

            val encryptionKey = getEncryptionKey()
            val salt = saltProvider.obtain()
            Timber.v("AES: key: $encryptionKey, salt: $salt")
            val enc = AES(encryptionKey, salt)
            Timber.i("Encryption: $enc")
            return enc

        } catch (e: IllegalStateException) {

            recordException(e)
            throw e
        }
    }

    fun setEncryptionKeyStorageIdentifier(identifier: String) {

        encryptionKey = identifier
    }

    fun addCallback(callback: TransmissionManagerEncryptionProviderCallback) {

        callbacks.register(callback)
    }

    fun removeCallback(callback: TransmissionManagerEncryptionProviderCallback) {

        callbacks.unregister(callback)
    }

    @Throws(IllegalStateException::class)
    private fun getEncryptionKey(): String {

        val count = manager.getScheduledCount()
        if (count == 0) {

            Timber.v("We are about to generate new signing key")

            var retryCount = 0
            val maxRetries = 10
            val key = UUID.randomUUID().toString()
            var persisted = Data.put(encryptionKey, key)

            while (!persisted && retryCount < maxRetries) {

                Thread.sleep(500)
                Timber.w("Persisting encryption key, retry no: %d", ++retryCount)
                persisted = Data.put(encryptionKey, key)
            }

            callback.onNewEncryptionKeyGenerated(key, persisted)

            if (persisted) {

                return key

            } else {

                throw IllegalStateException("Encryption key not persisted")
            }

        } else {

            Timber.v("We are about to obtain existing signing key")

            val key: String = Data[encryptionKey] ?: ""
            val success = !TextUtils.isEmpty(key)

            callback.onExistingEncryptionKeyObtained(key, success)

            if (!success) {

                throw IllegalStateException("Encryption key was expected")
            }

            return key
        }
    }
}