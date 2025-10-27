/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.redelf.commons.test

import com.redelf.commons.execution.Executor
import com.redelf.commons.extensions.CountDownLatch
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.Callable
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

            } catch (e: Throwable) {

                Assert.fail(e.message)
            }
        }

        Assert.assertEquals(expected * iterations, set.get())
    }
}