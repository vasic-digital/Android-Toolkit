package com.redelf.commons.creation.instantiation

interface Instantiable<T> {

    fun instantiate(): T
}