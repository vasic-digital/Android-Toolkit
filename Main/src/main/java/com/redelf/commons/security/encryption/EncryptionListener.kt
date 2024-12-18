package com.redelf.commons.security.encryption

interface EncryptionListener<IN, OUT> {

    fun onEncrypted(key: String, raw: IN, encrypted: OUT)

    fun onDecrypted(key: String, encrypted: OUT, decrypted: IN)
}