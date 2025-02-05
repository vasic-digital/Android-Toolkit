package com.redelf.commons.security.encryption

import java.security.GeneralSecurityException

interface Encryption<IN, OUT> {

    @Throws(GeneralSecurityException::class)
    fun encrypt(data: IN): OUT

    @Throws(GeneralSecurityException::class)
    fun decrypt(source: OUT): IN
}