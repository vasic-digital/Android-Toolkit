package com.redelf.commons.execution

import android.os.Handler
import android.os.Looper
import timber.log.Timber
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.ThreadPoolExecutor

enum class Executor : Execution {

    MAIN {

        var DEBUG = false

        private val cpus = CPUs()
        private val cores = cpus.numberOfCores

        private val capacity = if (cores * 3 <= 10) {

            10

        } else {

            cores * 3
        }

        private val executor = TaskExecutor.instantiate(capacity)

        override fun execute(what: Runnable) {

            logCapacity()

            Exec.execute(what, executor)
        }

        override fun <T> execute(callable: Callable<T>): Future<T> {

            logCapacity()

            return Exec.execute(callable, executor)
        }

        override fun execute(action: Runnable, delayInMillis: Long) {

            logCapacity()

            Exec.execute(action, delayInMillis, executor)
        }

        private fun logCapacity() {

            if (!DEBUG) {

                return
            }

            val maximumPoolSize = executor.maximumPoolSize
            val available = maximumPoolSize - executor.activeCount

            val msg = "${cpus.tag} Available=$available, Total=$maximumPoolSize"

            if (available > 0) {

                Timber.v(msg)

            } else {

                Timber.e(msg)
            }
        }
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
    };

    private object Exec {

        fun execute(action: Runnable, executor: ThreadPoolExecutor) {

            executor.execute(action)
        }

        fun <T> execute(callable: Callable<T>, executor: ThreadPoolExecutor): Future<T> {

            return executor.submit(callable)
        }

        fun execute(action: Runnable, delayInMillis: Long, executor: ThreadPoolExecutor) {

            executor.execute {

                try {

                    Thread.sleep(delayInMillis)
                    action.run()

                } catch (e: InterruptedException) {

                    Timber.e(e)
                }
            }
        }
    }
}