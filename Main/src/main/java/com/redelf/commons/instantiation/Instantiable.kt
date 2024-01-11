package com.redelf.commons.instantiation

interface Instantiable<T> {

    fun instantiate(): T
}