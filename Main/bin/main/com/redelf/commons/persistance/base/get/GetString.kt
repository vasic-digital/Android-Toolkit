package com.redelf.commons.persistance.base.get

interface GetString {

    fun getString(key: String, defaultValue: String): String
}