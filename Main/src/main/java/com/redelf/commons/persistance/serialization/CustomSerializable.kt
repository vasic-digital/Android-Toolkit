package com.redelf.commons.persistance.serialization

interface CustomSerializable {

    fun getCustomSerializations(): Map<String, String>
}