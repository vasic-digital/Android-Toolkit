package com.redelf.commons.test.execution

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.redelf.commons.execution.SecureRetrying
import com.redelf.commons.test.BaseTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class SecureRetryingTest : BaseTest() {

    @Test
    fun testSuccessfulOperationNoRetries() = runBlocking {
        val retrying = SecureRetrying.create(maxRetries = 3)
        val attempts = AtomicInteger(0)

        val result = retrying.execute {
            attempts.incrementAndGet()
            true // Success on first try
        }

        assertTrue("Operation should succeed", result.success)
        assertEquals("Should only attempt once", 1, result.attempts)
        assertEquals("Should only attempt once", 1, attempts.get())
        assertNull("Should have no exception", result.lastException)
    }

    @Test
    fun testRetryUntilSuccess() = runBlocking {
        val retrying = SecureRetrying.create(
            maxRetries = 5,
            baseDelayMs = 10,
            backoffStrategy = SecureRetrying.BackoffStrategy.FIXED
        )
        val attempts = AtomicInteger(0)

        val result = retrying.execute {
            val attempt = attempts.incrementAndGet()
            attempt >= 3 // Succeed on 3rd attempt
        }

        assertTrue("Operation should eventually succeed", result.success)
        assertEquals("Should attempt 3 times", 3, result.attempts)
        assertEquals("Should attempt 3 times", 3, attempts.get())
        assertNull("Should have no exception when successful", result.lastException)
    }

    @Test
    fun testMaxRetriesExceeded() = runBlocking {
        val retrying = SecureRetrying.create(
            maxRetries = 3,
            baseDelayMs = 10
        )
        val attempts = AtomicInteger(0)

        val result = retrying.execute {
            attempts.incrementAndGet()
            false // Always fail
        }

        assertFalse("Operation should fail after max retries", result.success)
        assertEquals("Should attempt max retries + 1", 4, result.attempts)
        assertEquals("Should attempt max retries + 1", 4, attempts.get())
    }

    @Test
    fun testExceptionHandling() = runBlocking {
        val retrying = SecureRetrying.create(
            maxRetries = 3,
            baseDelayMs = 10
        )
        val attempts = AtomicInteger(0)

        val result = retrying.execute {
            attempts.incrementAndGet()
            throw RuntimeException("Test exception")
        }

        assertFalse("Operation should fail due to exception", result.success)
        assertEquals("Should attempt max retries + 1", 4, result.attempts)
        assertEquals("Should attempt max retries + 1", 4, attempts.get())
        assertNotNull("Should have last exception", result.lastException)
        assertEquals("Exception message should match", "Test exception", result.lastException?.message)
    }

    @Test
    fun testMixedFailuresAndExceptions() = runBlocking {
        val retrying = SecureRetrying.create(
            maxRetries = 4,
            baseDelayMs = 10
        )
        val attempts = AtomicInteger(0)

        val result = retrying.execute {
            val attempt = attempts.incrementAndGet()
            when (attempt) {
                1 -> false // First attempt fails
                2 -> throw RuntimeException("Exception on attempt 2")
                3 -> false // Third attempt fails
                4 -> true // Fourth attempt succeeds
                else -> false
            }
        }

        assertTrue("Operation should succeed on 4th attempt", result.success)
        assertEquals("Should attempt 4 times", 4, result.attempts)
        assertEquals("Should attempt 4 times", 4, attempts.get())
        assertNull("Should have no exception when successful", result.lastException)
    }

    @Test
    fun testLinearBackoff() = runBlocking {
        val retrying = SecureRetrying.create(
            maxRetries = 3,
            baseDelayMs = 50,
            backoffStrategy = SecureRetrying.BackoffStrategy.LINEAR,
            jitterEnabled = false
        )
        val attempts = AtomicInteger(0)
        val timestamps = mutableListOf<Long>()

        val result = retrying.execute {
            timestamps.add(System.currentTimeMillis())
            val attempt = attempts.incrementAndGet()
            false // Always fail to test all delays
        }

        assertFalse("Should fail after all retries", result.success)
        assertEquals("Should have 4 timestamps", 4, timestamps.size)

        // Check delays (approximately linear: 50ms, 100ms, 150ms)
        val delay1 = timestamps[1] - timestamps[0]
        val delay2 = timestamps[2] - timestamps[1]
        val delay3 = timestamps[3] - timestamps[2]

        assertTrue("First delay should be around 50ms", delay1 >= 40 && delay1 <= 80)
        assertTrue("Second delay should be around 100ms", delay2 >= 80 && delay2 <= 140)
        assertTrue("Third delay should be around 150ms", delay3 >= 120 && delay3 <= 200)
    }

    @Test
    fun testExponentialBackoff() = runBlocking {
        val retrying = SecureRetrying.create(
            maxRetries = 3,
            baseDelayMs = 25,
            backoffStrategy = SecureRetrying.BackoffStrategy.EXPONENTIAL,
            jitterEnabled = false
        )
        val attempts = AtomicInteger(0)
        val timestamps = mutableListOf<Long>()

        val result = retrying.execute {
            timestamps.add(System.currentTimeMillis())
            val attempt = attempts.incrementAndGet()
            false // Always fail to test all delays
        }

        assertFalse("Should fail after all retries", result.success)

        // Check delays (approximately exponential: 25ms, 50ms, 100ms)
        val delay1 = timestamps[1] - timestamps[0]
        val delay2 = timestamps[2] - timestamps[1]
        val delay3 = timestamps[3] - timestamps[2]

        assertTrue("First delay should be around 25ms", delay1 >= 15 && delay1 <= 45)
        assertTrue("Second delay should be around 50ms", delay2 >= 35 && delay2 <= 75)
        assertTrue("Third delay should be around 100ms", delay3 >= 75 && delay3 <= 140)
    }

    @Test
    fun testFibonacciBackoff() = runBlocking {
        val retrying = SecureRetrying.create(
            maxRetries = 4,
            baseDelayMs = 20,
            backoffStrategy = SecureRetrying.BackoffStrategy.FIBONACCI,
            jitterEnabled = false
        )
        val attempts = AtomicInteger(0)
        val timestamps = mutableListOf<Long>()

        val result = retrying.execute {
            timestamps.add(System.currentTimeMillis())
            val attempt = attempts.incrementAndGet()
            false // Always fail to test all delays
        }

        assertFalse("Should fail after all retries", result.success)

        // Fibonacci sequence: 1, 1, 2, 3, 5
        // So delays should be: 20ms, 20ms, 40ms, 60ms, 100ms
        val delay1 = timestamps[1] - timestamps[0]
        val delay2 = timestamps[2] - timestamps[1]
        val delay3 = timestamps[3] - timestamps[2]
        val delay4 = timestamps[4] - timestamps[3]

        assertTrue("First Fibonacci delay", delay1 >= 10 && delay1 <= 35)
        assertTrue("Second Fibonacci delay", delay2 >= 10 && delay2 <= 35)
        assertTrue("Third Fibonacci delay", delay3 >= 25 && delay3 <= 60)
        assertTrue("Fourth Fibonacci delay", delay4 >= 45 && delay4 <= 85)
    }

    @Test
    fun testMaxDelayLimit() = runBlocking {
        val retrying = SecureRetrying.create(
            maxRetries = 3,
            baseDelayMs = 100,
            maxDelayMs = 150, // Limit delays to 150ms
            backoffStrategy = SecureRetrying.BackoffStrategy.EXPONENTIAL,
            jitterEnabled = false
        )
        val attempts = AtomicInteger(0)
        val timestamps = mutableListOf<Long>()

        val result = retrying.execute {
            timestamps.add(System.currentTimeMillis())
            val attempt = attempts.incrementAndGet()
            false // Always fail
        }

        // With exponential backoff: 100ms, 200ms (capped to 150ms), 400ms (capped to 150ms)
        val delay2 = timestamps[2] - timestamps[1]
        val delay3 = timestamps[3] - timestamps[2]

        assertTrue("Second delay should be capped", delay2 >= 130 && delay2 <= 170)
        assertTrue("Third delay should be capped", delay3 >= 130 && delay3 <= 170)
    }

    @Test
    fun testJitterVariation() = runBlocking {
        val retrying = SecureRetrying.create(
            maxRetries = 5,
            baseDelayMs = 100,
            backoffStrategy = SecureRetrying.BackoffStrategy.FIXED,
            jitterEnabled = true
        )
        val attempts = AtomicInteger(0)
        val delays = mutableListOf<Long>()
        val timestamps = mutableListOf<Long>()

        val result = retrying.execute {
            timestamps.add(System.currentTimeMillis())
            val attempt = attempts.incrementAndGet()
            false // Always fail to collect all delays
        }

        // Calculate actual delays
        for (i in 1 until timestamps.size) {
            delays.add(timestamps[i] - timestamps[i-1])
        }

        // With jitter enabled, delays should vary around the base delay
        val allDelaysSame = delays.all { it == delays[0] }
        assertFalse("Delays should vary with jitter enabled", allDelaysSame)

        // But all should be within reasonable range of base delay (75ms to 125ms for 100ms base)
        delays.forEach { delay ->
            assertTrue("Delay $delay should be within jitter range", delay >= 60 && delay <= 150)
        }
    }

    @Test
    fun testSyncExecution() {
        val retrying = SecureRetrying.create(
            maxRetries = 3,
            baseDelayMs = 10
        )
        val attempts = AtomicInteger(0)

        val result = retrying.executeSync {
            val attempt = attempts.incrementAndGet()
            attempt >= 2 // Succeed on 2nd attempt
        }

        assertTrue("Sync operation should succeed", result.success)
        assertEquals("Should attempt 2 times", 2, result.attempts)
        assertEquals("Should attempt 2 times", 2, attempts.get())
    }

    @Test
    fun testSecurityLimits() {
        // Test that security limits are enforced
        val retrying = SecureRetrying.create(
            maxRetries = 100, // Should be clamped to 10
            baseDelayMs = 100000, // Should be clamped to 30000
            maxDelayMs = 1000000 // Should be clamped to 300000
        )

        val attempts = AtomicInteger(0)
        val startTime = System.currentTimeMillis()

        val result = runBlocking {
            retrying.execute {
                attempts.incrementAndGet()
                false // Always fail to test max retries limit
            }
        }

        val totalTime = System.currentTimeMillis() - startTime

        assertFalse("Should fail after max retries", result.success)
        assertTrue("Should enforce max retries limit", result.attempts <= 11) // 10 retries + 1
        assertTrue("Should enforce reasonable time limits", totalTime < 60000) // Less than 1 minute
    }

    @Test
    fun testMetrics() {
        // Execute some operations to generate metrics
        val retrying1 = SecureRetrying.create(maxRetries = 2)
        val retrying2 = SecureRetrying.create(maxRetries = 2)

        runBlocking {
            // Successful operation
            retrying1.execute { true }
            
            // Failed operation
            retrying2.execute { false }
        }

        val metrics = SecureRetrying.getMetrics()

        assertNotNull("Metrics should not be null", metrics)
        assertTrue("Should have total retry attempts", (metrics["totalRetryAttempts"] as Long) > 0)
        assertTrue("Should have successful retries", (metrics["successfulRetries"] as Long) > 0)
        assertTrue("Should have failed retries", (metrics["failedRetries"] as Long) > 0)
        assertNotNull("Should have success rate", metrics["successRate"])
    }

    @Test
    fun testTimeoutHandling() = runBlocking {
        val retrying = SecureRetrying.create(
            maxRetries = 2,
            baseDelayMs = 10,
            timeoutPerAttemptMs = 100 // Very short timeout
        )
        val attempts = AtomicInteger(0)

        val result = retrying.execute {
            attempts.incrementAndGet()
            Thread.sleep(200) // Sleep longer than timeout
            true
        }

        assertFalse("Should fail due to timeouts", result.success)
        assertTrue("Should have attempted multiple times", result.attempts > 1)
        
        val metrics = SecureRetrying.getMetrics()
        assertTrue("Should have timeout retries", (metrics["timeoutRetries"] as Long) > 0)
    }

    @Test
    fun testResultDataAccessors() {
        val retrying = SecureRetrying.create(maxRetries = 1)

        val result = runBlocking {
            retrying.execute { false }
        }

        // Test all accessor methods
        assertFalse("isSuccess should return false", result.isSuccess())
        assertEquals("getAttempts should work", result.attempts, result.attempts)
        assertEquals("getDurationMs should work", result.totalDurationMs, result.getDurationMs())
        assertEquals("getLastException should work", result.lastException, result.lastException)
        assertTrue("Duration should be positive", result.getDurationMs() >= 0)
    }
}