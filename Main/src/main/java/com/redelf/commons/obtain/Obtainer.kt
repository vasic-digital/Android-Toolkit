package com.redelf.commons.obtain

interface Obtainer<T> {

    fun getObtainer(vararg params: Any): Obtain<T>
}