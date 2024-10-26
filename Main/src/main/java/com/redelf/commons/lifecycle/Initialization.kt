package com.redelf.commons.lifecycle

import com.redelf.commons.extensions.exec
import com.redelf.commons.lifecycle.exception.NotInitializedException
import com.redelf.commons.logging.Console
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

interface Initialization : InitializationCondition {

    fun initialize(): Boolean
}