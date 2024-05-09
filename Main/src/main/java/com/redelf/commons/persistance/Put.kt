package com.redelf.commons.persistance

interface Put {

    fun <T> put(key: String, value: T): Boolean
}