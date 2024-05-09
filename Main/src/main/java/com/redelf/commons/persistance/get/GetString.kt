package com.redelf.commons.persistance.get

interface GetString {

    fun getString(key: String, defaultValue: String): String
}