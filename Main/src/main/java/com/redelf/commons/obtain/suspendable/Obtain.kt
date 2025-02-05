package com.redelf.commons.obtain.suspendable

interface Obtain<T> {

    fun obtain(): T
}