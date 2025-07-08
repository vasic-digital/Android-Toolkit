package com.redelf.commons.obtain

interface ObtainAsync<T> {

    fun obtain(callback: OnObtain<T>)
}