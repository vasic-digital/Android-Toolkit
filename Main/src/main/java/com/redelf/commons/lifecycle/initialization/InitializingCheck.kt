package com.redelf.commons.lifecycle.initialization

interface InitializingCheck : InitializedCheck {

    fun isInitializing(): Boolean

    fun initializationCompleted(e: Exception? = null)
}