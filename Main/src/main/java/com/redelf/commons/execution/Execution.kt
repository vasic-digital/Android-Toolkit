package com.redelf.commons.execution

import java.util.concurrent.Callable
import java.util.concurrent.Future

interface Execution {

    fun execute(action: Runnable)

    fun <T> execute(callable: Callable<T>): Future<T>

    fun execute(action: Runnable, delayInMillis: Long)
}