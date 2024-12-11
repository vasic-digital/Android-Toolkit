package com.redelf.commons.persistance.serialization

class DefaultCustomSerializer(private val clazz: Class<*>) : Serializer {

    override fun takeClass(): Class<*> {

        return clazz
    }

    override fun serialize(key: String, value: Any) = true

    override fun deserialize(key: String) = null
}