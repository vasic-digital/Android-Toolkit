package com.redelf.commons.direction

interface Pull<K> {

    fun <T> pull(key: K): T?
}