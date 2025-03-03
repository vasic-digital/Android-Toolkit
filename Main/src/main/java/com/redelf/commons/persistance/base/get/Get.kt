package com.redelf.commons.persistance.base.get

interface Get<T> {

    fun get(key: String, defaultValue: T): T?
}