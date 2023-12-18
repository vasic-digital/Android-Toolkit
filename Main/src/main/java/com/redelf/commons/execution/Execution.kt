package com.redelf.commons.execution

interface Execution {

    fun execute(action: Runnable)

    fun execute(action: Runnable, delayInMillis: Long)
}