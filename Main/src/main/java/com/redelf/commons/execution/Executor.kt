package com.redelf.commons.execution

import android.os.Handler
import android.os.Looper
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicBoolean

enum class Executor : Execution, Performer<Executor> {

    MAIN {

        val DEBUG = AtomicBoolean()

        private val cpus = CPUs()
        private val cores = cpus.numberOfCores

        private val capacity = if (cores * 3 <= 10) {

            100

        } else {

            cores * 3 * 10
        }

        val executor = TaskExecutor.instantiate(capacity)

        override fun getPerformer() = executor

        override fun execute(what: Runnable) {

            logCapacity()

            Exec.execute(what, executor)
        }

        override fun <T> execute(callable: Callable<T>): Future<T>? {

            logCapacity()

            return Exec.execute(callable, executor)
        }

        override fun execute(action: Runnable, delayInMillis: Long) {

            logCapacity()

            Exec.execute(action, delayInMillis, executor)
        }

        private fun logCapacity() {

            if (!DEBUG.get()) {

                return
            }

            val maximumPoolSize = executor.maximumPoolSize
            val available = maximumPoolSize - executor.activeCount

            val msg = "${cpus.tag} Available=$available, Total=$maximumPoolSize"

            if (available > 0) {

                Console.log(msg)

            } else {

                Console.error(msg)
            }
        }
    },

    SINGLE {

        private val executor = TaskExecutor.instantiateSingle()

        override fun execute(what: Runnable) {

            Exec.execute(what, executor)
        }

        override fun <T> execute(callable: Callable<T>): Future<T>? {

            return Exec.execute(callable, executor)
        }

        override fun execute(action: Runnable, delayInMillis: Long) {

            Exec.execute(action, delayInMillis, executor)
        }

        override fun getPerformer() = executor
    },

    UI {

        private val executor = Handler(Looper.getMainLooper())

        @Throws(IllegalStateException::class)
        override fun execute(what: Runnable) {

            if (!executor.post(what)) {

                throw IllegalStateException("Could not accept action")
            }
        }

        @Throws(IllegalStateException::class)
        override fun <T> execute(callable: Callable<T>): Future<T> {

            val action = FutureTask(callable)

            execute(action)

            return action
        }

        @Throws(IllegalStateException::class)
        override fun execute(action: Runnable, delayInMillis: Long) {

            if (!executor.postDelayed(action, delayInMillis)) {

                throw IllegalStateException("Could not accept action")
            }
        }

        /*
        * TODO: Think about more proper solution for this
        * */
        @Throws(UnsupportedOperationException::class)
        override fun getPerformer() =
            throw UnsupportedOperationException("Cannot get performer for UI executor")
    };

    private object Exec {

        fun execute(action: Runnable, executor: ThreadPoolExecutor) {

            try {

                executor.execute(action)

            } catch (e: RejectedExecutionException) {

                recordException(e)
            }
        }

        fun <T> execute(callable: Callable<T>, executor: ThreadPoolExecutor): Future<T>? {

            try {

                return executor.submit(callable)

            } catch (e: RejectedExecutionException) {

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

                    } catch (e: InterruptedException) {

                        Console.error(e)
                    }
                }

            } catch (e: RejectedExecutionException) {

                recordException(e)
            }
        }
    }
}