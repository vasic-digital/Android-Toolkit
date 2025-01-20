package com.redelf.commons.execution

import android.os.Handler
import android.os.Looper
import com.redelf.commons.logging.Console
import kotlinx.coroutines.*
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicBoolean

enum class Executor : Execution {

    /*
    * TODO: Check for potential exceptions
    * */

    MAIN {

        val DEBUG = AtomicBoolean()
        val THREAD_POOLED = AtomicBoolean()

        private val cores = CPUs().numberOfCores

        private val capacity = if (cores * 3 <= 10) {

            100

        } else {

            cores * 3 * 10
        }

        private val executor = TaskExecutor.instantiate(capacity)

        @OptIn(DelicateCoroutinesApi::class)
        override fun execute(what: Runnable) {

            if (THREAD_POOLED.get()) {

                logCapacity()

                Exec.execute(what, executor)

            } else {

                GlobalScope.launch(Dispatchers.Default) {

                    what.run()
                }
            }
        }

        @OptIn(DelicateCoroutinesApi::class)
        override fun <T> execute(callable: Callable<T>): T {

            if (THREAD_POOLED.get()) {

                logCapacity()

                return Exec.execute(callable, executor).get()

            } else {

                val job = GlobalScope.async(Dispatchers.Default) {

                    callable.call()
                }

                return runBlocking {

                    job.await()
                }
            }
        }

        @OptIn(DelicateCoroutinesApi::class)
        override fun execute(action: Runnable, delayInMillis: Long) {

            if (THREAD_POOLED.get()) {

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

            if (!DEBUG.get()) {

                return
            }

            val maximumPoolSize = executor.maximumPoolSize
            val available = maximumPoolSize - executor.activeCount

            val msg = "${CPUs.tag} Available=$available, Total=$maximumPoolSize"

            if (available > 0) {

                Console.log(msg)

            } else {

                Console.error(msg)
            }
        }
    },

    SINGLE {

        val THREAD_POOLED = AtomicBoolean()

        private val executor = TaskExecutor.instantiateSingle()

        @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
        override fun execute(what: Runnable) {

            if (THREAD_POOLED.get()) {

                Exec.execute(what, executor)

            } else {

                GlobalScope.launch(Dispatchers.Default.limitedParallelism(1)) {

                    what.run()
                }
            }
        }

        @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
        override fun <T> execute(callable: Callable<T>): T {

            if (THREAD_POOLED.get()) {

                return Exec.execute(callable, executor).get()

            } else {

                val job = GlobalScope.async(Dispatchers.Default.limitedParallelism(1)) {

                    callable.call()
                }

                return runBlocking {

                    job.await()
                }
            }
        }

        @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
        override fun execute(action: Runnable, delayInMillis: Long) {

            if (THREAD_POOLED.get()) {

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

        @Throws(IllegalStateException::class)
        override fun <T> execute(callable: Callable<T>): T {

            val action = FutureTask(callable)

            execute(action)

            return action.get()
        }

        @Throws(IllegalStateException::class)
        override fun execute(action: Runnable, delayInMillis: Long) {

            if (!executor.postDelayed(action, delayInMillis)) {

                throw IllegalStateException("Could not accept action")
            }
        }

        @Throws(IllegalStateException::class)
        override fun execute(what: Runnable) {

            if (!executor.post(what)) {

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

                    Console.error(e)
                }
            }
        }
    }
}