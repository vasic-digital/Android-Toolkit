package com.redelf.commons.persistance.base

interface Encryption<T> {

    fun init(): Boolean

    @Throws(Exception::class)
    fun encrypt(key: String, value: String): T?

    @Throws(Exception::class)
    fun decrypt(key: String, value: T): String?
}