package com.redelf.commons.persistance

class NoEncryption : Encryption {
    override fun init(): Boolean {

        return true
    }

    @Throws(Exception::class)
    override fun encrypt(key: String?, value: String?): ByteArray? {

        return value?.toByteArray()
    }

    @Throws(Exception::class)
    override fun decrypt(key: String?, value: ByteArray?): String {

        value?.let {

            return String(it)
        }

        return ""
    }
}