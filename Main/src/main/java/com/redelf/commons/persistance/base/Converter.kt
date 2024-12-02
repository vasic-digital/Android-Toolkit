package com.redelf.commons.persistance.base

import com.redelf.commons.persistance.DataInfo
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicBoolean

interface Converter {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    fun <T> toString(value: T): String?

    fun <T> fromString(value: String?, type: Type?): T?

    fun <T> fromString(value: String?, clazz: Class<T>?): T?

    fun <T> fromString(value: String?, info: DataInfo?): T?
}
