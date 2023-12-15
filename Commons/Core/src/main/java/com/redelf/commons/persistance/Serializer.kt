package com.redelf.commons.persistance

interface Serializer {

    fun <T> serialize(cipherText: ByteArray?, value: T): String?

    fun deserialize(plainText: String?): DataInfo?
}