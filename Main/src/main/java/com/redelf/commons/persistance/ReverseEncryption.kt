package com.redelf.commons.persistance

import com.redelf.commons.extensions.hashCodeString
import com.redelf.commons.persistance.base.Encryption
import com.redelf.commons.persistance.base.Salter

class ReverseEncryption(

    salter: Salter

) : Encryption<String> {

    private val salt = salter.getSalt()
    private val separator = salt.reversed().hashCodeString().toString().reversed().substring(0, 2)

    override fun init() = true

    @Throws(Exception::class)
    override fun encrypt(key: String, value: String): String? {

        return ("${key.hashCodeString()}${separator}${salt}${separator}${value.reversed()}" +
                "${separator}${salt}${separator}${key.hashCodeString()}")
    }

    @Throws(Exception::class)
    override fun decrypt(key: String, value: String): String {

        return value
            .replace("${key.hashCodeString()}${separator}", "")
            .replace("${salt}${separator}", "")
            .replace("${separator}${salt}", "")
            .replace("${separator}${key.hashCodeString()}", "")
            .reversed()
    }
}