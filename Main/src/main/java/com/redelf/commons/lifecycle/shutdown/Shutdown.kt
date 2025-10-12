package com.redelf.commons.lifecycle.shutdown

import com.redelf.commons.lifecycle.LifecycleCallback

interface Shutdown<T> {

    fun shutdown(callback: LifecycleCallback<T>)
}