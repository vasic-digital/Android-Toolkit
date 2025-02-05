package com.redelf.commons.security.encryption

import com.redelf.commons.json.JsonSerialization
import java.security.GeneralSecurityException

interface Encrypt : JsonSerialization {

    @Throws(GeneralSecurityException::class, OutOfMemoryError::class)
    fun encrypt(encryption: Encryption<String, String>): String {

        val json = toJson()

        return encryption.encrypt(json)
    }
}