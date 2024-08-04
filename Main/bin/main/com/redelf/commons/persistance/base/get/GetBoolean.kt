package com.redelf.commons.persistance.base.get

interface GetBoolean {

    fun getBoolean(key: String, defaultValue: Boolean): Boolean
}