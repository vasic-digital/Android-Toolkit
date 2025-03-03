package com.redelf.commons.obtain.suspendable

interface ObtainSuspendable<T> {

    suspend fun obtain(): T
}