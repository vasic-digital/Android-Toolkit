package com.redelf.commons.security.encryption

import com.redelf.commons.json.JsonDeserialization
import java.security.GeneralSecurityException

interface Decrypt<T> : JsonDeserialization<T> {

    @Throws(GeneralSecurityException::class, IllegalArgumentException::class)
    fun decrypt(source: String, encryption: Encryption<String, String>): T {

        val json = encryption.decrypt(source)
        return fromJson(json)
    }
}