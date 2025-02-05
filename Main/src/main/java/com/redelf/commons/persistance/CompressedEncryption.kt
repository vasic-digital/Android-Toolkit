package com.redelf.commons.persistance

import com.redelf.commons.extensions.compressAndEncrypt
import com.redelf.commons.extensions.decryptAndDecompress
import com.redelf.commons.persistance.base.Encryption

class CompressedEncryption() : Encryption<String> {

    override fun init() = true

    override fun encrypt(key: String, value: String): String? {

        return value.compressAndEncrypt()
    }

    override fun decrypt(key: String, value: String): String? {

        return value.decryptAndDecompress()
    }
}