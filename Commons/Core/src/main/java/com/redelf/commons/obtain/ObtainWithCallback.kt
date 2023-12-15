package com.redelf.commons.obtain

interface ObtainWithCallback<T> {

    fun obtain(callback: OnObtain<T>)
}