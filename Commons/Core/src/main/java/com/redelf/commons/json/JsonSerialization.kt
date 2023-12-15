package com.redelf.commons.json

import com.google.gson.Gson

interface JsonSerialization {

    @Throws(OutOfMemoryError::class)
    fun toJson(): String {

        val gson = Gson()

        return gson.toJson(this)
    }
}