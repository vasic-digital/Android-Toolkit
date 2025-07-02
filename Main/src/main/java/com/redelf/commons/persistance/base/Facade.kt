package com.redelf.commons.persistance.base

import com.redelf.commons.lifecycle.InitializationWithContext
import com.redelf.commons.lifecycle.ShutdownSynchronized
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.obtain.OnObtain

import java.lang.reflect.Type

interface Facade : ShutdownSynchronized, TerminationSynchronized, InitializationWithContext {
    fun <T> put(key: String?, value: T): Boolean

    fun <T> get(key: String?, callback: OnObtain<T?>)

    fun <T> get(key: String?, defaultValue: T, callback: OnObtain<T?>)

    fun getByType(key: String?, type: Type, callback: OnObtain<Any?>)

    fun getByClass(key: String?, clazz: Class<*>, callback: OnObtain<Any?>)

    fun count(): Long

    fun deleteAll(): Boolean

    fun delete(key: String?): Boolean

    fun contains(key: String?, callback: OnObtain<Boolean>)
}
