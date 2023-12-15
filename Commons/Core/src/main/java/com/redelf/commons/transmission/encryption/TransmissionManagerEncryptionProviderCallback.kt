package com.redelf.commons.transmission.encryption

interface TransmissionManagerEncryptionProviderCallback {

    fun onNewEncryptionKeyGenerated(key: String, success: Boolean)

    fun onExistingEncryptionKeyObtained(key: String, success: Boolean)
}