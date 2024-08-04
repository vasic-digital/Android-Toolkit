package com.redelf.commons.lifecycle

interface Shutdown<T> {

    fun shutdown(callback: LifecycleCallback<T>)
}