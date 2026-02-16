package com.redelf.commons.persistance.base

import com.redelf.commons.lifecycle.initialization.InitializationWithContext
import com.redelf.commons.lifecycle.shutdown.ShutdownSynchronized
import com.redelf.commons.lifecycle.termination.TerminationSynchronized
import com.redelf.commons.obtain.OnObtain

/*
    KNOWN LIMITATION: This interface extends both ShutdownSynchronized and TerminationSynchronized,
    which have overlapping responsibilities. A future refactoring should consolidate these into
    a single lifecycle interface across the codebase.
*/
interface Storage<T> : ShutdownSynchronized, TerminationSynchronized, InitializationWithContext {

    fun put(key: String?, value: T): Boolean

    fun get(key: String?, callback: OnObtain<T?>)

    fun delete(key: String?): Boolean

    fun deleteAll(): Boolean

    fun count(): Long

    fun contains(key: String?, callback: OnObtain<Boolean?>)
}
