package com.redelf.commons.persistance

interface EncryptedPersistenceListener {

    fun onEncrypted(key: String, raw: String, encrypted: String, )

    fun onDecrypted(key: String, encrypted: String, decrypted: String, )
}