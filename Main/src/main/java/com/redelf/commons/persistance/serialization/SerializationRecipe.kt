package com.redelf.commons.persistance.serialization

data class SerializationRecipe<T>(

    val clazz: Class<T>,
    val serializer: Serializer<T>? = null
)
