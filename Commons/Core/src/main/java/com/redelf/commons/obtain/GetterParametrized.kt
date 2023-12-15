package com.redelf.commons.obtain

interface GetterParametrized<T, P> {

    fun get(param: P): T
}