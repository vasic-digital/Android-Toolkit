package com.redelf.commons.lifecycle.initialization

interface InitializationParametrizedSync<T, P> : InitializationCondition {

    fun initialize(param: P): T
}