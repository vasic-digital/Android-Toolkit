package com.redelf.commons.extensions

import com.google.gson.Gson
import com.redelf.commons.logging.Console

// FIXME: Shall use GsonParser with custom serialization support
private val gson = Gson()

fun json(what: Any): String {

    try {

        return gson.toJson(what)

    } catch (e: Exception) {

        Console.error(e)
    }

    return ""
}

fun Any.toJson(): String {

    return json(this)
}