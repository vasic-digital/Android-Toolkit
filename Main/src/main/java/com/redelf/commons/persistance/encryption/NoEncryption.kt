package com.redelf.commons.persistance.encryption

import com.redelf.commons.persistance.base.Encryption

class NoEncryption : Encryption<String> {

    override fun init() = true

    @Throws(Exception::class)
    override fun encrypt(key: String, value: String) = value

    @Throws(Exception::class)
    override fun decrypt(key: String, value: String) = value
}