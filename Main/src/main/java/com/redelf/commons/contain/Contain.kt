package com.redelf.commons.contain

fun interface Contain<K> {

    fun contains(key: K): Boolean
}