package com.redelf.commons.lifecycle

interface InitializationCondition {

    fun isInitialized(): Boolean

    fun isNotInitialized() = !isInitialized()

    fun isInitializing(): Boolean

    fun initializationCompleted(e: Exception? = null)
}