package com.redelf.commons.lifecycle

interface InitializationParametrizedSync<T, P> : InitializationCondition {

    fun initialize(param: P): T
}