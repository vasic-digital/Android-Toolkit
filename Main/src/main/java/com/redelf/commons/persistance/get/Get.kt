package com.redelf.commons.persistance.get

interface Get<T> {

    fun get(key: String, defaultValue: T): T?
}