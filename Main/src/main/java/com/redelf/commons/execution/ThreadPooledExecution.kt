package com.redelf.commons.execution

import java.util.concurrent.ThreadPoolExecutor

interface ThreadPooledExecution {

    fun isThreadPooledExecution(): Boolean

    fun toggleThreadPooledExecution(enabled: Boolean)

    fun instantiateExecutor(): ThreadPoolExecutor
}