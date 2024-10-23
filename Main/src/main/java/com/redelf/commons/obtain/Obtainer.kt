package com.redelf.commons.obtain

interface Obtainer<T> {

    fun getObtainer(): Obtain<T>
}