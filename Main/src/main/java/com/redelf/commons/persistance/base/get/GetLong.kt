package com.redelf.commons.persistance.base.get

interface GetLong {

    fun getLong(key: String, defaultValue: Long): Long
}