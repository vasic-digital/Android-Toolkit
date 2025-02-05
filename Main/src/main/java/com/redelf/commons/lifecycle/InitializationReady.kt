package com.redelf.commons.lifecycle

interface InitializationReady {

    fun canInitialize(): Boolean

    fun initializationReady(): Boolean
}