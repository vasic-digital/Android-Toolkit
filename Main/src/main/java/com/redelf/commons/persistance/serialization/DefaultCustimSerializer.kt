package com.redelf.commons.persistance.serialization

class DefaultCustomSerializer : Serializer<Any> {

    override fun serialize(key: String, byteArray: Any) = true

    override fun deserialize(key: String) = null
}