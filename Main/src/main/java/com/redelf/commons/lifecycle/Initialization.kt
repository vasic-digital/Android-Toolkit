package com.redelf.commons.lifecycle

import com.redelf.commons.exec
import com.redelf.commons.execution.Executor
import com.redelf.commons.lifecycle.exception.NotInitializedException
import timber.log.Timber
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.jvm.Throws

interface Initialization<T> : InitializationCondition {

    companion object {

        @Throws(NotInitializedException::class)
        fun waitForInitialization(

            who: Initialization<*>,
            timeoutInSeconds: Long = 30L,
            initLogTag: String = "${who::class.simpleName} initialization ::"

        ) {

            val callable = object : Callable<Boolean> {

                override fun call(): Boolean {

                    if (who.isInitializing()) {

                        Timber.w("$initLogTag still initializing")
                    }

                    while (who.isInitializing()) {

                        Thread.yield()
                    }

                    if (who.isNotInitialized()) {

                        Thread.yield()
                    }

                    if (who.isInitialized()) {

                        Timber.v("$initLogTag initialized")

                        return true
                    }

                    return false
                }
            }

            exec(

                callable = callable,
                timeout = timeoutInSeconds,
                timeUnit = TimeUnit.SECONDS,
                logTag = initLogTag
            )

            if (!who.isInitialized()) {

                throw NotInitializedException()
            }
        }
    }

    fun initialize(callback: LifecycleCallback<T>)
}