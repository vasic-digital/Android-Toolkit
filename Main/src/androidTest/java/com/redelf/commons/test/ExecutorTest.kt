package com.redelf.commons.test

import com.redelf.commons.execution.Executor
import org.junit.Test

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

        // TODO: Implement test cases
    }
}