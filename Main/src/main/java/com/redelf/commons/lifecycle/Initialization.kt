package com.redelf.commons.lifecycle

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
            initLogTag: String = "${who::class.simpleName} initialization ::"

        ) {

            val callable = object : Callable<Boolean> {

                override fun call(): Boolean {

                    if (who.isInitializing()) {

                        Timber.w("$initLogTag still initializing")
                    }

                    if (who.isInitializing()) {

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

            val future = Executor.MAIN.execute(callable)

            try {

                val tag = "$initLogTag Init check ::"

                Timber.v("$tag PRE-START")

                val success = future.get(30, TimeUnit.SECONDS)

                if (success) {

                    Timber.v("$tag Callable: RETURNED SUCCESS")

                } else {

                    Timber.e("$tag Callable: RETURNED FAILURE")
                }

                Timber.v("$tag Callable: POST-END")

            } catch (e: RejectedExecutionException) {

                Timber.e(e)

            } catch (e: InterruptedException) {

                Timber.e(e)

            } catch (e: ExecutionException) {

                Timber.e(e)

            } catch (e: TimeoutException) {

                future.cancel(true)
            }

            if (!who.isInitialized()) {

                throw NotInitializedException()
            }
        }
    }

    fun initialize(callback: LifecycleCallback<T>)
}