package com.redelf.commons.lifecycle

interface TerminationParametrized<P, T> {

    fun terminate(param: P, callback: LifecycleCallback<T>)
}