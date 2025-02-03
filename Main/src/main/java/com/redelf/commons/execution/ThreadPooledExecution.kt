package com.redelf.commons.execution

import java.util.concurrent.ThreadPoolExecutor

interface ThreadPooledExecution {

    fun instantiateExecutor(): ThreadPoolExecutor
}