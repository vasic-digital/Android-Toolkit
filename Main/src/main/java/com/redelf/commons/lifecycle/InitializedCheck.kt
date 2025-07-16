package com.redelf.commons.lifecycle

interface InitializedCheck {

    fun isInitialized(): Boolean

    fun isNotInitialized() = !isInitialized()
}