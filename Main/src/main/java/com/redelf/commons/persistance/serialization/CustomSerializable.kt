package com.redelf.commons.persistance.serialization

interface CustomSerializable {

    /*
    * TODO:
    *  - Make sure that we can provide recipe for the whole type
    *  - Vs. just the fields
    */

    fun getCustomSerializations(): Map<String, SerializationRecipe<*>>
}