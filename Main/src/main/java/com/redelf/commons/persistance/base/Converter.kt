package com.redelf.commons.persistance.base

import com.redelf.commons.persistance.DataInfo
import java.lang.reflect.Type

interface Converter {

    fun <T> toString(value: T): String?

    fun <T> fromString(value: String?, type: Type?): T?

    fun <T> fromString(value: String?, info: DataInfo?): T?
}
