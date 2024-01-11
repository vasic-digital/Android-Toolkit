package com.redelf.commons.instantiation

interface InstantiableParametrized<in R, out T> {

    fun instantiate(from: R): T
}