package com.redelf.commons.obtain

interface OnObtain<T> {

    fun onCompleted(data: T)

    fun onFailure(error: Throwable)
}