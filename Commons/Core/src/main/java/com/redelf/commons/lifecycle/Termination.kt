package com.redelf.commons.lifecycle

interface Termination<T> {

    fun shutdown(callback: LifecycleCallback<T>)
}