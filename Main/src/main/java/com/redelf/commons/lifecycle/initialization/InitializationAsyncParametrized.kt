package com.redelf.commons.lifecycle.initialization

import com.redelf.commons.lifecycle.LifecycleCallback

interface InitializationAsyncParametrized<T, P> : InitializationCondition {

    fun initialize(param: P, callback: LifecycleCallback<T>)
}