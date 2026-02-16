package com.redelf.commons.persistance.serialization

interface CustomSerializable {

    /*
    * NOTE: Planned enhancements for custom serialization:
    *  - Annotation-based version of this interface (e.g., @CustomSerialization)
    *  - Support getCustomSerializations via annotations as well
    *  - Allow providing serialization recipe for an entire type, not just individual fields
    */

    fun getCustomSerializations(): Map<String, Serializer>
}