package com.redelf.commons.persistance.base

import com.redelf.commons.persistance.DataInfo

interface Converter {

    fun <T> toString(value: T): String?

    fun <T> fromString(value: String?, dataInfo: DataInfo?): T?
}
