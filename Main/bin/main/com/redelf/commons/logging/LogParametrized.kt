package com.redelf.commons.logging

interface LogParametrized {

    fun logParametrized(priority: Int, tag: String?, message: String, t: Throwable?)
}