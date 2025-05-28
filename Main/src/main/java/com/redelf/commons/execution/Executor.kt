package com.redelf.commons.execution

import android.os.Handler
import android.os.Looper
import com.redelf.commons.Debuggable
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import kotlinx.coroutines.*
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicBoolean

enum class Executor : Execution, ThreadPooledExecution, Debuggable {

    MAIN {

        private val debug = AtomicBoolean()
        private val tag = "Executor :: MAIN ::"
        private val cores = CPUs().numberOfCores
        private val threadPooled = AtomicBoolean()

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

        @OptIn(DelicateCoroutinesApi::class)
        override fun execute(what: Runnable) { // TODO: Instead of Runnable use Runnable and ApiRunnable

            if (isDebug()) Console.log("$tag START :: threadPooled = ${isThreadPooledExecution()}")

            if (threadPooled.get()) {

                if (isDebug()) Console.log("$tag PRE-EXECUTING")

                logCapacity()

                if (isDebug()) Console.log("$tag EXECUTING")

                Exec.execute(what, executor)

                if (isDebug()) Console.log("$tag SENT TO EXECUTOR")

            } else {

                if (isDebug()) Console.log("$tag LAUNCHING")

                /*
                * FIXME: In certain scenarios it gets stuck
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
        }

        @OptIn(DelicateCoroutinesApi::class)
        override fun <T> execute(callable: Callable<T>): T? {

            if (threadPooled.get()) {

                logCapacity()

                try {

                    return Exec.execute(callable, executor)?.get()

                } catch (e: Throwable) {

                    recordException(e)
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
        override fun execute(action: Runnable, delayInMillis: Long) {

            if (threadPooled.get()) {

                logCapacity()

                Exec.execute(action, delayInMillis, executor)

            } else {

                GlobalScope.launch(Dispatchers.Default) {

                    delay(delayInMillis)

                    action.run()
                }
            }
        }

        private fun logCapacity() {

            if (!debug.get()) {

                return
            }

            val maximumPoolSize = executor.maximumPoolSize
            val available = maximumPoolSize - executor.activeCount

            val msg = "${CPUs.tag} Available = $available, Total = $maximumPoolSize"

            if (available > 0) {

                Console.log(msg)

            } else {

                Console.error(msg)
            }
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
        override fun execute(what: Runnable) {

            if (threadPooled.get()) {

                Exec.execute(what, executor)

            } else {

                GlobalScope.launch(Dispatchers.Default.limitedParallelism(1)) {

                    what.run()
                }
            }
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

        override fun execute(what: Runnable) {

            if (!executor.post(what)) {

                val e = IllegalStateException("Could not accept action")
                recordException(e)
            }
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