package com.redelf.commons.creation.instantiation

interface Instantiable<T> {

    fun instantiate(vararg params: Any): T
}