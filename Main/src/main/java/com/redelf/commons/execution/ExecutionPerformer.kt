package com.redelf.commons.execution

import com.redelf.commons.obtain.Obtain
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Future

interface ExecutionPerformer<P> {

    fun getPerformer(): P
}