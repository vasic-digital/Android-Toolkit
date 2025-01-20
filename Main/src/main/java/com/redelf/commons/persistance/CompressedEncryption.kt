package com.redelf.commons.persistance

import com.redelf.commons.persistance.base.Encryption
import com.redelf.commons.persistance.base.Salter

class CompressedEncryption(

    salter: Salter

) : Encryption<String> {

    private val salt = salter.getSalt()

    override fun init(): Boolean {

        TODO("Not yet implemented")
    }

    override fun encrypt(key: String, value: String): String? {

        TODO("Not yet implemented")
    }

    override fun decrypt(key: String, value: String): String? {

        TODO("Not yet implemented")
    }
}