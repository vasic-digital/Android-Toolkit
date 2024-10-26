package com.redelf.commons.lifecycle

import com.redelf.commons.extensions.exec
import com.redelf.commons.lifecycle.exception.NotInitializedException
import com.redelf.commons.logging.Console
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

interface InitializationAsync<T> : InitializationCondition {

    companion object {

        @Throws(NotInitializedException::class)
        fun waitForInitialization(

            who: InitializationAsync<*>,
            timeoutInSeconds: Long = 60L,
            initLogTag: String = "${who::class.simpleName} initialization ::"

        ) {

            val callable = object : Callable<Boolean> {

                override fun call(): Boolean {

                    if (who.isNotInitialized()) {

                        Console.log("$initLogTag not initialized yet")
                    }

                    if (who.isInitializing()) {

                        Console.log("$initLogTag still initializing")
                    }

                    while (who.isInitializing()) {

                        Thread.yield()
                    }

                    if (who.isNotInitialized()) {

                        Thread.yield()
                    }

                    if (who.isInitialized()) {

                        Console.log("$initLogTag initialized")

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