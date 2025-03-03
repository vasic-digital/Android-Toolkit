package com.redelf.commons.lifecycle

interface ShutdownCallback<T> {

    fun onShutdown(success: Boolean, vararg args: T)
}