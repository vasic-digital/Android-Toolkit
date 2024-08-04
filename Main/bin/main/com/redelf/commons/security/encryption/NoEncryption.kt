package com.redelf.commons.security.encryption

class NoEncryption : Encryption<String, String> {

    override fun encrypt(data: String) = data

    override fun decrypt(source: String) = source
}