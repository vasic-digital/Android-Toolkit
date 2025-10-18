package com.redelf.commons.callback

fun interface ErrorCallback {

    fun onError(error: Throwable)
}