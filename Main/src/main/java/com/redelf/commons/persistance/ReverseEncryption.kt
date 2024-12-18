package com.redelf.commons.persistance

import android.content.Context
import com.facebook.android.crypto.keychain.AndroidConceal
import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain
import com.facebook.crypto.Crypto
import com.facebook.crypto.CryptoConfig
import com.facebook.crypto.Entity
import com.facebook.crypto.keychain.KeyChain
import com.redelf.commons.logging.Console
import com.redelf.commons.persistance.base.Encryption
import com.redelf.commons.persistance.base.Salter

class ReverseEncryption constructor(

    private val salter: Salter

) : Encryption {

    private val tag = "Encryption :: Reverse ::"

    private val salt = salter.getSalt()

    override fun init() = true

    @Throws(Exception::class)
    override fun encrypt(key: String?, value: String?): ByteArray? {

        return ("${key.hashCode()}###${salter.getSalt()}###${value?.reversed()}" +
                "###${salter.getSalt()}###${key.hashCode()}").toByteArray()
    }

    @Throws(Exception::class)
    override fun decrypt(key: String?, value: ByteArray?): String {

        value?.let {

            return String(it)
                .replace("${key.hashCode()}###", "")
                .replace("${salter.getSalt()}###", "")
                .replace("###${salter.getSalt()}", "")
                .replace("###${key.hashCode()}", "")
                .reversed()
        }

        return ""
    }

    private fun getKey(key: String?): String {

        val raw = (key + salt).hashCode().toString().toCharArray()

        return "${raw.last()}"
    }
}