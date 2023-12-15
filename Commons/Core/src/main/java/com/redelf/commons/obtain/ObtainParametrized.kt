package com.redelf.commons.obtain

interface ObtainParametrized<T, P> {

    fun obtain(param: P): T
}