package com.redelf.commons.obtain

interface ObtainSuspendable<T> {

    suspend fun obtain(): T
}