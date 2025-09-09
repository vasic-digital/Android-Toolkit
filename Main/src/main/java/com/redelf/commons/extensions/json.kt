package com.redelf.commons.extensions

import com.redelf.commons.logging.Console
import com.redelf.commons.persistance.StreamingJsonParser

// High-performance streaming JSON parser - replaced unsafe Gson
private val streamingParser = StreamingJsonParser.instantiate(
    "json-extensions",
    null,
    false
)

fun json(what: Any): String {
    return try {
        streamingParser.toJson(what) ?: ""
    } catch (e: Throwable) {
        Console.error("JSON serialization failed: ${e.message}")
        Console.error(e)
        ""
    }
}

fun Any.toJson(): String {

    return json(this)
}