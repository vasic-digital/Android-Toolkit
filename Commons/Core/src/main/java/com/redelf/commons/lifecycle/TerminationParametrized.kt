package com.redelf.commons.lifecycle

interface TerminationParametrized<P, T> {

    fun shutdown(param: P, callback: LifecycleCallback<T>)
}