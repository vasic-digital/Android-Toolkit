package com.redelf.commons.destruction.reset

interface ResettableParametrized<T> : Resetting {

    fun reset(arg: T): Boolean
}