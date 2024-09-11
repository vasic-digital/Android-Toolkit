package com.redelf.commons.creation.instantiation

interface InstantiableParametrized<in R, out T> {

    fun instantiate(from: R): T
}