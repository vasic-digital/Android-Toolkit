package com.redelf.commons.lifecycle.initialization

interface InitializedCheck {

    fun isInitialized(): Boolean

    fun isNotInitialized() = !isInitialized()
}