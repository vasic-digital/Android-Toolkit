package com.redelf.commons.persistance.serialization

interface Serializer<T> {

    fun serialize(key: String, byteArray: T): Boolean

    fun deserialize(key: String): T?
}