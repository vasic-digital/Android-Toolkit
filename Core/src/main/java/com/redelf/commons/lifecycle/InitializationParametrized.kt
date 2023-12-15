package com.redelf.commons.lifecycle

interface InitializationParametrized<T, P> : InitializationCondition {

    fun initialize(param: P, callback: LifecycleCallback<T>)
}