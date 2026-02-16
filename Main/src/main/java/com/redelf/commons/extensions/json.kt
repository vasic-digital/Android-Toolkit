package com.redelf.commons.extensions

import com.google.gson.Gson
import com.redelf.commons.logging.Console

// NOTE: Uses a plain Gson instance for lightweight JSON conversion in extension functions.
//  GsonParser with custom serialization support is used in the persistence layer instead.
//  This intentional separation keeps these utility functions dependency-free.
private val gson = Gson()

fun json(what: Any): String {

    try {

        return gson.toJson(what)

    } catch (e: Throwable) {

        Console.error(e)
    }

    return ""
}

fun Any.toJson(): String {

    return json(this)
}