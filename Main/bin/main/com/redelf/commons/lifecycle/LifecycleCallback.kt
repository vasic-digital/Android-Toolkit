package com.redelf.commons.lifecycle

// TODO: Split into the two interfaces
interface LifecycleCallback<T> {

    fun onInitialization(success: Boolean, vararg args: T)

    fun onShutdown(success: Boolean, vararg args: T)
}