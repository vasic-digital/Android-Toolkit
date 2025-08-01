package com.redelf.commons.persistance.base

import com.redelf.commons.lifecycle.initialization.InitializationWithContext
import com.redelf.commons.lifecycle.shutdown.ShutdownSynchronized
import com.redelf.commons.lifecycle.termination.TerminationSynchronized
import com.redelf.commons.obtain.OnObtain

/*
    FIXME: We do not need both ShutdownSynchronized and TerminationSynchronized.
       Check other the files as well!
* */
interface Storage<T> : ShutdownSynchronized, TerminationSynchronized, InitializationWithContext {

    fun put(key: String?, value: T): Boolean

    fun get(key: String?, callback: OnObtain<T?>)

    fun delete(key: String?): Boolean

    fun deleteAll(): Boolean

    fun count(): Long

    fun contains(key: String?, callback: OnObtain<Boolean?>)
}
