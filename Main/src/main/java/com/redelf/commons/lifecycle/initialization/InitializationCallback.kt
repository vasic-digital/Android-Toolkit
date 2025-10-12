package com.redelf.commons.lifecycle.initialization

interface InitializationCallback<T> {

    fun onInitialization(success: Boolean, vararg args: T)
}