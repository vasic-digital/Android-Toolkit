package com.redelf.commons.execution

import android.os.Handler
import android.os.Looper
import com.redelf.commons.Debuggable
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicBoolean

enum class Executor : Execution, ThreadPooledExecution, Debuggable {

    MAIN {

        private val debug = AtomicBoolean()
        private val tag = "Executor :: MAIN ::"
        private val cores = CPUs().numberOfCores
        private val threadPooled = AtomicBoolean(true)

        private val capacity = if (cores * 3 <= 10) {

            100

        } else {

            cores * 3 * 10
        }

        private val executor = instantiateExecutor()

        override fun toggleThreadPooledExecution(enabled: Boolean) {

            threadPooled.set(enabled)
        }

        override fun isThreadPooledExecution() = threadPooled.get()

        override fun instantiateExecutor() = TaskExecutor.instantiate(capacity)

        /*
        * TODO:
        *  - Instead of Runnable use Runnable and ApiRunnable
        *  - Wrap all CountDown latched with existing sync {} extension
        *  - Make sure that sync {} extension can work with countdown latch with multiple count downs
        *  - Make sure that sync {} extension works with coroutines when threadPooled.get() is false
        */
        @OptIn(DelicateCoroutinesApi::class)
        @Throws(RejectedExecutionException::class)
        override fun execute(what: Runnable): Boolean {

            if (isDebug()) Console.log("$tag START :: threadPooled = ${isThreadPooledExecution()}")

            if (threadPooled.get()) {

                if (isDebug()) Console.log("$tag PRE-EXECUTING")

                if (checkCapacity()) {

                    if (isDebug()) Console.log("$tag EXECUTING")

                    Exec.execute(what, executor)

                    if (isDebug()) Console.log("$tag SENT TO EXECUTOR")

                } else {

                    throw RejectedExecutionException("No capacity to execute action")
                }

            } else {

                if (isDebug()) Console.log("$tag LAUNCHING")

                /*
                * FIXME: In certain scenarios it gets stuck [any yield or latch await ...]
                */
                GlobalScope.launch(Dispatchers.Default) {

                    if (isDebug()) Console.log("$tag EXECUTING")

                    try {

                        what.run()

                    } catch (e: Throwable) {

                        recordException(e)
                    }

                    if (isDebug()) Console.log("$tag EXECUTED")
                }

                if (isDebug()) Console.log("$tag END")
            }

            return true
        }

        @OptIn(DelicateCoroutinesApi::class)
        @Throws(RejectedExecutionException::class)
        override fun <T> execute(callable: Callable<T>): T? {

            if (threadPooled.get()) {

                if (checkCapacity()) {

                    try {

                        return Exec.execute(callable, executor)?.get()

                    } catch (e: Throwable) {

                        recordException(e)
                    }

                } else {

                    throw RejectedExecutionException("No capacity to execute action")
                }


            } else {

                val job = GlobalScope.async(Dispatchers.Default) {

                    try {

                        callable.call()

                    } catch (e: Throwable) {

                        recordException(e)
                    }

                    null
                }

                return runBlocking {

                    job.await()
                }
            }

            return null
        }

        @OptIn(DelicateCoroutinesApi::class)
        @Throws(RejectedExecutionException::class)
        override fun execute(action: Runnable, delayInMillis: Long) {

            if (threadPooled.get()) {

                if (checkCapacity()) {

                    Exec.execute(action, delayInMillis, executor)

                } else {

                    throw RejectedExecutionException("No capacity to execute action")
                }

            } else {

                GlobalScope.launch(Dispatchers.Default) {

                    delay(delayInMillis)

                    action.run()
                }
            }
        }

        private fun checkCapacity(): Boolean {

            val maximumPoolSize = executor.maximumPoolSize
            val available = maximumPoolSize - executor.activeCount
            val isAvailable = available > 1

            if (debug.get()) {

                val msg = "${CPUs.tag} Available = $available, Total = $maximumPoolSize"

                if (isAvailable) {

                    Console.log(msg)

                } else {

                    Console.error(msg)
                }
            }

            return isAvailable
        }

        override fun setDebug(debug: Boolean) {

            this.debug.set(debug)
        }

        override fun isDebug(): Boolean {

            return debug.get()
        }
    },

    SINGLE {

        private val debug = AtomicBoolean()
        private val threadPooled = AtomicBoolean()
        private val executor = instantiateExecutor()

        override fun toggleThreadPooledExecution(enabled: Boolean) {

            threadPooled.set(enabled)
        }

        override fun isThreadPooledExecution() = threadPooled.get()

        override fun instantiateExecutor() = TaskExecutor.instantiateSingle()

        @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
        override fun execute(what: Runnable): Boolean {

            if (threadPooled.get()) {

                Exec.execute(what, executor)

            } else {

                GlobalScope.launch(Dispatchers.Default.limitedParallelism(1)) {

                    what.run()
                }
            }

            return true
        }

        @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
        override fun <T> execute(callable: Callable<T>): T? {

            if (threadPooled.get()) {

                try {

                    return Exec.execute(callable, executor)?.get()

                } catch (e: Throwable) {

                    recordException(e)
                }

            } else {

                val job = GlobalScope.async(Dispatchers.Default.limitedParallelism(1)) {

                    try {

                        callable.call()

                    } catch (e: Throwable) {

                        recordException(e)
                    }

                    null
                }

                return runBlocking {

                    job.await()
                }
            }

            return null
        }

        @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
        override fun execute(action: Runnable, delayInMillis: Long) {

            if (threadPooled.get()) {

                Exec.execute(action, delayInMillis, executor)

            } else {

                GlobalScope.launch(Dispatchers.Default.limitedParallelism(1)) {

                    delay(delayInMillis)

                    action.run()
                }
            }
        }

        override fun setDebug(debug: Boolean) {

            this.debug.set(debug)
        }

        override fun isDebug(): Boolean {

            return debug.get()
        }
    },

    UI {

        private val debug = AtomicBoolean()
        private val executor = Handler(Looper.getMainLooper())

        override fun <T> execute(callable: Callable<T>): T? {

            try {

                val action = FutureTask(callable)

                execute(action)

                return action.get()

            } catch (e: Throwable) {

                recordException(e)
            }

            return null
        }

        @Throws(IllegalStateException::class)
        override fun execute(action: Runnable, delayInMillis: Long) {

            if (!executor.postDelayed(action, delayInMillis)) {

                val e = IllegalStateException("Could not accept action")
                recordException(e)
            }
        }

        override fun execute(what: Runnable): Boolean {

            return executor.post(what)
        }

        override fun isThreadPooledExecution() = false

        @Throws(UnsupportedOperationException::class)
        override fun toggleThreadPooledExecution(enabled: Boolean) {

            throw UnsupportedOperationException("Not supported")
        }


        @Throws(UnsupportedOperationException::class)
        override fun instantiateExecutor() = throw UnsupportedOperationException("Not supported")

        override fun setDebug(debug: Boolean) {

            this.debug.set(debug)
        }

        override fun isDebug(): Boolean {

            return debug.get()
        }
    };

    private object Exec {

        fun execute(action: Runnable, executor: ThreadPoolExecutor) {

            try {

                executor.execute(action)

                // TODO: Make sure we can use this idea
                //                if (isOnMainThread()) {
                //
                //                    executor.execute(action)
                //
                //                } else {
                //
                //                    action.run()
                //                }


            } catch (e: Throwable) {

                recordException(e)
            }
        }

        fun <T> execute(callable: Callable<T>, executor: ThreadPoolExecutor): Future<T>? {

            try {

                return executor.submit(callable)

            } catch (e: Throwable) {

                recordException(e)
            }

            return null
        }

        fun execute(action: Runnable, delayInMillis: Long, executor: ThreadPoolExecutor) {

            try {

                executor.execute {

                    try {

                        Thread.sleep(delayInMillis)
                        action.run()

                    } catch (e: Throwable) {

                        recordException(e)
                    }
                }

            } catch (e: Throwable) {

                recordException(e)
            }
        }
    }
}