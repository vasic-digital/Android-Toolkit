package com.redelf.commons.extensions

import com.redelf.commons.logging.Console

fun Throwable.log(): String {

    return message ?: this::class.java.simpleName
}

fun Throwable.log(pattern: String, error: Boolean = true) {

    val msg = String.format(pattern, log())

    if (error) {

        Console.error(msg)

    } else {

        Console.warning(msg)
    }
}