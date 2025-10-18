package com.redelf.commons.callback

fun interface CallbackOperation<T> {
    fun perform(callback: T)
}