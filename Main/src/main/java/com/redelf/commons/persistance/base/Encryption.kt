package com.redelf.commons.persistance.base

interface Encryption {

    fun init(): Boolean

    @Throws(Exception::class)
    fun encrypt(key: String?, value: String?): ByteArray?

    @Throws(Exception::class)
    fun decrypt(key: String?, value: ByteArray?): String?
}