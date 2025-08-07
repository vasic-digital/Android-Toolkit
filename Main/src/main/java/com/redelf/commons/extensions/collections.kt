package com.redelf.commons.extensions

import java.util.concurrent.ConcurrentHashMap

fun <K, V> ConcurrentHashMap<K, V>.removeByValue(value: V): Boolean {

    val iterator = this.iterator()

    while (iterator.hasNext()) {

        val entry = iterator.next()

        if (entry.value == value) {

            iterator.remove()

            return true
        }
    }

    return false
}