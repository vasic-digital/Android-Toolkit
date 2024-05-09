package com.redelf.commons.persistance

interface Get {

    fun <T> get(key: String, defaultValue: T): Any?
}