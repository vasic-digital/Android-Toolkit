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

fun <K, V> ConcurrentHashMap<K, V>.removeAllByValue(value: V): Boolean {

    var removed = false
    val iterator = this.iterator()

    while (iterator.hasNext()) {

        if (iterator.next().value == value) {

            iterator.remove()
            removed = true
        }
    }

    return removed
}