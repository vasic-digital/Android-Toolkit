package com.redelf.commons.persistance.base

import com.redelf.commons.persistance.DataInfo

interface Serializer {

    fun <T> serialize(cipherText: ByteArray?, value: T): String?

    fun deserialize(plainText: String?): DataInfo?
}