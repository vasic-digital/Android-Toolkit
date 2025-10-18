package com.redelf.commons.creation.instantiation

fun interface InstantiableParametrized<in R, out T> {

    fun instantiate(from: R): T
}