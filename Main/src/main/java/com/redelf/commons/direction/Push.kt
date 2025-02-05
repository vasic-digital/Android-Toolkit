package com.redelf.commons.direction

interface Push<K> {

    fun <T> push(key: K, what: T): Boolean
}