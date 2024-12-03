package com.redelf.commons.persistance.serialization

class DefaultCustomSerializer : Serializer {

    override fun serialize(key: String, value: Any) = true

    override fun deserialize(key: String) = null
}