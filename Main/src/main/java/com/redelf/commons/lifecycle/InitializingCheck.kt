package com.redelf.commons.lifecycle

interface InitializingCheck : InitializedCheck {

    fun isInitializing(): Boolean

    fun initializationCompleted(e: Exception? = null)
}