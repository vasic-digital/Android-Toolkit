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

class ReverseEncryption constructor(salter: Salter) : Encryption<String> {

    private val salt = salter.getSalt()
    private val tag = "Encryption :: Reverse ::"

    override fun init() = true

    @Throws(Exception::class)
    override fun encrypt(key: String, value: String): String? {

        return ("${key.hashCode()}###${salt}###${value.reversed()}" +
                "###${salt}###${key.hashCode()}")
    }

    @Throws(Exception::class)
    override fun decrypt(key: String, value: String): String {

        return value
            .replace("${key.hashCode()}###", "")
            .replace("${salt}###", "")
            .replace("###${salt}", "")
            .replace("###${key.hashCode()}", "")
            .reversed()
    }
}