package com.redelf.commons.lifecycle.initialization

interface InitializationReady {

    fun canInitialize(): Boolean

    fun initializationReady(): Boolean
}