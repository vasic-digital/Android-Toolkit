package com.redelf.commons.lifecycle

interface InitializationCallback<T> {

    fun onInitialization(success: Boolean, vararg args: T)
}