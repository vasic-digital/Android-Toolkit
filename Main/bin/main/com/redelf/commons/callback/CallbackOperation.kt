package com.redelf.commons.callback

interface CallbackOperation<T> {
    fun perform(callback: T)
}