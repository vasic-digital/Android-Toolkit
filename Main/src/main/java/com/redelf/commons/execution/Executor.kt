package com.redelf.commons.execution

import android.os.Handler
import android.os.Looper
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import kotlinx.coroutines.*
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicBoolean

enum class Executor : Execution, ThreadPooledExecution {

    MAIN {

        val debug = AtomicBoolean()
        val threadPooled = AtomicBoolean()

        private val cores = CPUs().numberOfCores

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
        override fun execute(what: Runnable) {

            if (threadPooled.get()) {

                logCapacity()

                Exec.execute(what, executor)

            } else {

                GlobalScope.launch(Dispatchers.Default) {

                    what.run()
                }
            }
        }

        @OptIn(DelicateCoroutinesApi::class)
        override fun <T> execute(callable: Callable<T>): T? {

            if (threadPooled.get()) {

                logCapacity()

                try {

                    return Exec.execute(callable, executor)?.get()

                } catch (e: Exception) {

                    recordException(e)
                }

            } else {

                val job = GlobalScope.async(Dispatchers.Default) {

                    try {

                        callable.call()

                    } catch (e: Exception) {

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
    },

    SINGLE {

        val threadPooled = AtomicBoolean()

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

                } catch (e: Exception) {

                    recordException(e)
                }

            } else {

                val job = GlobalScope.async(Dispatchers.Default.limitedParallelism(1)) {

                    try {

                        callable.call()

                    } catch (e: Exception) {

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
    },

    UI {

        private val executor = Handler(Looper.getMainLooper())

        override fun <T> execute(callable: Callable<T>): T? {

            try {

                val action = FutureTask(callable)

                execute(action)

                return action.get()

            } catch (e: Exception) {

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
    };

    private object Exec {

        fun execute(action: Runnable, executor: ThreadPoolExecutor) {

            try {

                executor.execute(action)

            } catch (e: Exception) {

                recordException(e)
            }
        }

        fun <T> execute(callable: Callable<T>, executor: ThreadPoolExecutor): Future<T>? {

            try {

                return executor.submit(callable)

            } catch (e: Exception) {

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

                    } catch (e: Exception) {

                        recordException(e)
                    }
                }

            } catch (e: Exception) {

                recordException(e)
            }
        }
    }
}