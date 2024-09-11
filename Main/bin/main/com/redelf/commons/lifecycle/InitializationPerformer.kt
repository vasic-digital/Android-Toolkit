package com.redelf.commons.lifecycle

interface InitializationPerformer {

    fun initialization(): Boolean

    fun onInitializationCompleted()

    fun onInitializationFailed(e: Exception)
}