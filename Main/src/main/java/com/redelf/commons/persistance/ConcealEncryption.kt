package com.redelf.commons.persistance

import android.content.Context
import com.facebook.android.crypto.keychain.AndroidConceal
import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain
import com.facebook.crypto.Crypto
import com.facebook.crypto.CryptoConfig
import com.facebook.crypto.Entity
import com.facebook.crypto.keychain.KeyChain
import com.redelf.commons.logging.Timber

class ConcealEncryption constructor(

    private val crypto: Crypto,
    salter: Salter

) : Encryption {
    constructor(context: Context, salter: Salter) :
            this(SharedPrefsBackedKeyChain(context, CryptoConfig.KEY_256), salter)

    constructor(keyChain: KeyChain?, salter: Salter) : this(

        AndroidConceal.get().createDefaultCrypto(keyChain), salter
    )

    private val salt = salter.getSalt()

    override fun init(): Boolean {

        return crypto.isAvailable
    }

    @Throws(Exception::class)
    override fun encrypt(key: String?, value: String?): ByteArray? {

        val entity = Entity.create(getKey(key))
        val encrypted = crypto.encrypt(value?.toByteArray(), entity)

        if (encrypted.isEmpty()) {

            Timber.w("Encrypted value is empty")
        }

        return encrypted
    }

    @Throws(Exception::class)
    override fun decrypt(key: String?, value: ByteArray?): String {

        val entity = Entity.create(getKey(key))
        val decrypted = crypto.decrypt(value, entity)

        Timber.v("Decrypted: ${decrypted.isNotEmpty()}")

        return String(decrypted)
    }

    private fun getKey(key: String?): String {

        val raw = (key + salt).hashCode().toString().toCharArray()

        return "${raw.last()}"
    }
}