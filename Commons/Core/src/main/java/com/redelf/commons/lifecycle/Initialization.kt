package com.redelf.commons.lifecycle

interface Initialization<T> : InitializationCondition {

    fun initialize(callback: LifecycleCallback<T>)
}