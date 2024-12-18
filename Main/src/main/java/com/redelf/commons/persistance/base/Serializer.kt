package com.redelf.commons.persistance.base

import com.redelf.commons.persistance.DataInfo

interface Serializer {

    fun <T> serialize(cipherText: String?, value: T): String?

    fun deserialize(plainText: String?): DataInfo?
}