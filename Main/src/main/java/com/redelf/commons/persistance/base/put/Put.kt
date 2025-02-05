package com.redelf.commons.persistance.base.put

interface Put<T> {

    fun put(key: String, value: T): Boolean
}