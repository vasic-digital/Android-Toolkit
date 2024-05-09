package com.redelf.commons.persistance

interface LogInterceptor {

    fun onLog(message: String?)

    fun onError(message: String?)
}
