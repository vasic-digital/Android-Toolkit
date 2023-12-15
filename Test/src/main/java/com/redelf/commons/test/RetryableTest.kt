package com.redelf.commons.test

import org.junit.Assert
import org.junit.Test
import com.redelf.commons.execution.Retryable

class RetryableTest : BaseTest() {

    private fun failure(): Boolean = false

    private fun success(): Boolean = true

    @Test
    fun testRetryable() {

        val expected = 10
        val retryable = Retryable(expected)

        var count = retryable.execute(this::failure)
        Assert.assertEquals(count, expected)

        count = retryable.execute(this::success)
        Assert.assertEquals(0, count)

        var current = 0
        val countUntil = 3

        count = retryable.execute {

            current++
            current == countUntil
        }

        Assert.assertEquals(countUntil - 1, count)
    }
}