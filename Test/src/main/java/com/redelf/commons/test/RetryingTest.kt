package com.redelf.commons.test

import com.redelf.commons.execution.Retrying
import org.junit.Assert
import org.junit.Test

class RetryingTest : BaseTest() {

    private fun failure(): Boolean = false

    private fun success(): Boolean = true

    @Test
    fun testRetryable() {

        val expected = 10
        val retrying = Retrying(expected)

        var count = retrying.execute(this::failure)
        Assert.assertEquals(count, expected)

        count = retrying.execute(this::success)
        Assert.assertEquals(0, count)

        var current = 0
        val countUntil = 3

        count = retrying.execute {

            current++
            current == countUntil
        }

        Assert.assertEquals(countUntil - 1, count)
    }
}