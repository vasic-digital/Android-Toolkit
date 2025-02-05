package com.redelf.commons.execution

import java.util.concurrent.Callable

interface Execution : Execute<Runnable> {

    fun <T> execute(callable: Callable<T>): T?

    fun execute(action: Runnable, delayInMillis: Long)
}