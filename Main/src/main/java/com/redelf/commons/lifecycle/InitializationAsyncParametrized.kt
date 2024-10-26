package com.redelf.commons.lifecycle

interface InitializationAsyncParametrized<T, P> : InitializationCondition {

    fun initialize(param: P, callback: LifecycleCallback<T>)
}