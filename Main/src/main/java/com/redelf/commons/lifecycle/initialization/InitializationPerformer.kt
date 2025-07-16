package com.redelf.commons.lifecycle.initialization

interface InitializationPerformer {

    fun initialization(): Boolean

    fun onInitializationCompleted()

    fun onInitializationFailed(e: Exception)
}