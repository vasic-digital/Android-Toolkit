package com.redelf.commons.extensions

fun Long.toDays(): Long {

    return this / (1000 * 60 * 60 * 24)
}

fun Int.toMillis(): Long {

    return this.toLong() * (1000 * 60 * 60 * 24)
}