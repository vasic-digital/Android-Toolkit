package com.redelf.commons.test

import com.redelf.commons.execution.Executor
import kotlinx.coroutines.Runnable
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ExecutorTest : BaseTest() {

    @Test
    fun testExecution() {

        doTestCasesMain(true)
        doTestCasesMain(false)
        doTestCasesSingle(true)
        doTestCasesSingle(false)
    }

    private fun doTestCasesMain(pooled: Boolean) = doTestCases(pooled, true)

    private fun doTestCasesSingle(pooled: Boolean) = doTestCases(pooled, false)

    private fun doTestCases(pooled: Boolean, main: Boolean) {

        val executor = if (main) {

            Executor.MAIN

        } else {

            Executor.SINGLE
        }

        val default = executor.isThreadPooledExecution()

        executor.toggleThreadPooledExecution(pooled)

        runTestCases(executor)

        executor.toggleThreadPooledExecution(default)
    }

    private fun runTestCases(executor: Executor) {

        val expected = 3
        val iterations = 10
        val set = AtomicInteger()

        (0 until iterations).forEach { i ->

            val latch = CountDownLatch(expected)

            val action = Runnable {

                set.incrementAndGet()
                latch.countDown()
            }

            executor.execute {

                action.run()
            }

            executor.execute(

                action = action,
                delayInMillis = 10
            )

            val callable = Callable {

                action.run()
            }

            executor.execute(callable)

            try {

                val timeOk = latch.await(30, TimeUnit.SECONDS)
                val timeout = !timeOk

                if (timeout) {

                    Assert.fail("Timeout")
                }

            } catch (e: Exception) {

                Assert.fail(e.message)
            }
        }

        Assert.assertEquals(expected * iterations, set.get())
    }
}