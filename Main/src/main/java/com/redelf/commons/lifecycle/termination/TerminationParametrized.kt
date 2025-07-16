package com.redelf.commons.lifecycle.termination

import com.redelf.commons.lifecycle.LifecycleCallback

interface TerminationParametrized<P, T> {

    fun shutdown(param: P, callback: LifecycleCallback<T>)
}