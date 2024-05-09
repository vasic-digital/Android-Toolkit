package com.redelf.commons.persistance.put

interface Put<T> {

    fun put(key: String, value: T): Boolean
}