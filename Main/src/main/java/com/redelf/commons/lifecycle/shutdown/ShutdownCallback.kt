package com.redelf.commons.lifecycle.shutdown

interface ShutdownCallback<T> {

    fun onShutdown(success: Boolean, vararg args: T)
}