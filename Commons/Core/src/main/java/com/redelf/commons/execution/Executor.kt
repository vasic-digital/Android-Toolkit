package com.redelf.commons.execution

import android.os.Handler
import android.os.Looper
import timber.log.Timber


enum class Executor : Execution {

    MAIN {

        private val executor = TaskExecutor.instantiate(5)

        override fun execute(action: Runnable) = Exec.execute(action, executor)

        override fun execute(action: Runnable, delayInMillis: Long) {

            Exec.execute(action, delayInMillis, executor)
        }
    },

    UI {
        private val executor = Handler(Looper.getMainLooper())

        @Throws(IllegalStateException::class)
        override fun execute(action: Runnable) {

            if (!executor.post(action)) {
                throw IllegalStateException("Could not accept action")
            }
        }

        @Throws(IllegalStateException::class)
        override fun execute(action: Runnable, delayInMillis: Long) {

            if (!executor.postDelayed(action, delayInMillis)) {
                throw IllegalStateException("Could not accept action")
            }
        }
    };

    private object Exec {

        fun execute(action: Runnable, executor: TaskExecutor) {

            executor.execute(action)
        }

        fun execute(action: Runnable, delayInMillis: Long, executor: TaskExecutor) {

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