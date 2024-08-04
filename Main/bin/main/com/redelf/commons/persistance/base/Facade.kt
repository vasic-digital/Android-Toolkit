package com.redelf.commons.persistance.base

import com.redelf.commons.lifecycle.InitializationWithContext
import com.redelf.commons.lifecycle.ShutdownSynchronized
import com.redelf.commons.lifecycle.TerminationSynchronized

import java.lang.reflect.Type

interface Facade : ShutdownSynchronized, TerminationSynchronized, InitializationWithContext {
    fun <T> put(key: String?, value: T): Boolean

    fun <T> get(key: String?): T?

    fun <T> get(key: String?, defaultValue: T): T

    fun getByType(key: String?, type: Type): Any?

    fun getByClass(key: String?, clazz: Class<*>): Any?

    fun count(): Long

    fun deleteAll(): Boolean

    fun delete(key: String?): Boolean

    fun contains(key: String?): Boolean

    fun destroy()
}
