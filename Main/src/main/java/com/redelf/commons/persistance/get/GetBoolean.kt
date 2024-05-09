package com.redelf.commons.persistance.get

interface GetBoolean {

    fun getBoolean(key: String, defaultValue: Boolean): Boolean
}