package com.redelf.commons.creation.instantiation

fun interface Instantiable<T> {

    fun instantiate(vararg params: Any): T
}