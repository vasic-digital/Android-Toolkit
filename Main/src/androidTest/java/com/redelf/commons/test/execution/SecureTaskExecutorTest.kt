package com.redelf.commons.test.execution

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.redelf.commons.execution.SecureTaskExecutor
import com.redelf.commons.test.BaseTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class SecureTaskExecutorTest : BaseTest() {

    private val executorsToClean = mutableListOf<SecureTaskExecutor>()

    @After
    fun cleanup() {
        executorsToClean.forEach { it.close() }
        executorsToClean.clear()
    }

    @Test
    fun testBasicExecution() {
        val executor = SecureTaskExecutor.create("test-basic", 2, 4, 10)
        executorsToClean.add(executor)

        val latch = CountDownLatch(1)
        val executed = AtomicBoolean(false)

        val success = executor.execute(Runnable {
            executed.set(true)
            latch.countDown()
        })

        assertTrue("Task should be accepted", success)
        assertTrue("Task should complete within timeout", latch.await(5, TimeUnit.SECONDS))
        assertTrue("Task should have executed", executed.get())
    }

    @Test
    fun testCallableExecution() {
        val executor = SecureTaskExecutor.create("test-callable", 2, 4, 10)
        executorsToClean.add(executor)

        val expectedResult = "test-result"
        val latch = CountDownLatch(1)

        val result = executor.execute(Callable {
            latch.countDown()
            expectedResult
        })

        assertTrue("Task should complete within timeout", latch.await(5, TimeUnit.SECONDS))
        assertEquals("Result should match", expectedResult, result)
    }

    @Test
    fun testDelayedExecution() {
        val executor = SecureTaskExecutor.create("test-delayed", 2, 4, 10)
        executorsToClean.add(executor)

        val latch = CountDownLatch(1)
        val executed = AtomicBoolean(false)
        val startTime = System.currentTimeMillis()

        val success = executor.execute(Runnable {
            executed.set(true)
            latch.countDown()
        }, 100) // 100ms delay

        assertTrue("Task should be accepted", success)
        assertTrue("Task should complete within timeout", latch.await(5, TimeUnit.SECONDS))
        assertTrue("Task should have executed", executed.get())

        val elapsedTime = System.currentTimeMillis() - startTime
        assertTrue("Task should be delayed by at least 100ms", elapsedTime >= 100)
    }

    @Test
    fun testConcurrentExecution() {
        val executor = SecureTaskExecutor.create("test-concurrent", 4, 8, 50)
        executorsToClean.add(executor)

        val taskCount = 20
        val latch = CountDownLatch(taskCount)
        val counter = AtomicInteger(0)
        val executedTasks = AtomicInteger(0)

        repeat(taskCount) {
            val success = executor.execute(Runnable {
                counter.incrementAndGet()
                // Simulate some work
                Thread.sleep(10)
                executedTasks.incrementAndGet()
                latch.countDown()
            })
            assertTrue("All tasks should be accepted", success)
        }

        assertTrue("All tasks should complete within timeout", latch.await(10, TimeUnit.SECONDS))
        assertEquals("All tasks should have started", taskCount, counter.get())
        assertEquals("All tasks should have completed", taskCount, executedTasks.get())
    }

    @Test
    fun testQueueCapacityLimits() {
        // Create executor with very limited capacity
        val executor = SecureTaskExecutor.create(
            "test-capacity", 
            corePoolSize = 1, 
            maximumPoolSize = 1, 
            queueCapacity = 2
        )
        executorsToClean.add(executor)

        val blockingLatch = CountDownLatch(1)
        val resultLatch = CountDownLatch(1)
        val rejectedTasks = AtomicInteger(0)

        // First task - will be executing
        val success1 = executor.execute(Runnable {
            try {
                blockingLatch.await(5, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        })
        assertTrue("First task should be accepted", success1)

        // Fill the queue (2 tasks)
        val success2 = executor.execute(Runnable { resultLatch.countDown() })
        val success3 = executor.execute(Runnable { resultLatch.countDown() })
        
        assertTrue("Second task should be accepted", success2)
        assertTrue("Third task should be accepted", success3)

        // This should be rejected due to queue limits
        repeat(5) {
            val success = executor.execute(Runnable { })
            if (!success) {
                rejectedTasks.incrementAndGet()
            }
        }

        // Release the blocking task
        blockingLatch.countDown()
        
        assertTrue("Some tasks should be rejected", rejectedTasks.get() > 0)
    }

    @Test
    fun testTimeoutHandling() {
        val executor = SecureTaskExecutor.create("test-timeout", 2, 4, 10)
        executorsToClean.add(executor)

        val latch = CountDownLatch(1)
        val timedOut = AtomicBoolean(false)

        // Execute task with very short timeout
        val result = executor.execute(Callable {
            try {
                Thread.sleep(1000) // Sleep longer than timeout
                "completed"
            } catch (e: InterruptedException) {
                "interrupted"
            }
        }, timeoutMillis = 100) // 100ms timeout

        // Task should timeout and return null
        assertNull("Task should timeout and return null", result)
    }

    @Test
    fun testRejectionPolicyCallerRuns() {
        val executor = SecureTaskExecutor.create(
            "test-caller-runs",
            corePoolSize = 1,
            maximumPoolSize = 1,
            queueCapacity = 1,
            rejectionPolicy = SecureTaskExecutor.RejectionPolicy.CALLER_RUNS
        )
        executorsToClean.add(executor)

        val blockingLatch = CountDownLatch(1)
        val executedTasks = AtomicInteger(0)
        val currentThread = Thread.currentThread()
        val executionThread = AtomicReference<Thread>()

        // Fill executor and queue
        executor.execute(Runnable {
            try {
                blockingLatch.await(5, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        })

        executor.execute(Runnable { executedTasks.incrementAndGet() })

        // This should run in caller thread due to CALLER_RUNS policy
        executor.execute(Runnable {
            executionThread.set(Thread.currentThread())
            executedTasks.incrementAndGet()
        })

        // Release blocking task
        blockingLatch.countDown()

        // Wait a bit for tasks to complete
        Thread.sleep(100)

        assertEquals("Task should have run in caller thread", 
                    currentThread, executionThread.get())
        assertEquals("Some tasks should have executed", 2, executedTasks.get())
    }

    @Test
    fun testRejectionPolicyDiscard() {
        val executor = SecureTaskExecutor.create(
            "test-discard",
            corePoolSize = 1,
            maximumPoolSize = 1,
            queueCapacity = 1,
            rejectionPolicy = SecureTaskExecutor.RejectionPolicy.DISCARD
        )
        executorsToClean.add(executor)

        val blockingLatch = CountDownLatch(1)
        val executedTasks = AtomicInteger(0)

        // Fill executor and queue
        executor.execute(Runnable {
            try {
                blockingLatch.await(5, TimeUnit.SECONDS)
                executedTasks.incrementAndGet()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        })

        val success1 = executor.execute(Runnable { executedTasks.incrementAndGet() })
        assertTrue("Second task should be accepted", success1)

        // These should be discarded
        val success2 = executor.execute(Runnable { executedTasks.incrementAndGet() })
        val success3 = executor.execute(Runnable { executedTasks.incrementAndGet() })

        // Tasks should be "accepted" but discarded silently
        assertTrue("Tasks should appear accepted with DISCARD policy", success2)
        assertTrue("Tasks should appear accepted with DISCARD policy", success3)

        // Release blocking task
        blockingLatch.countDown()

        // Wait for completion
        Thread.sleep(200)

        // Only the first two tasks should have actually executed
        assertEquals("Only queued tasks should execute", 2, executedTasks.get())
    }

    @Test
    fun testHasCapacity() {
        val executor = SecureTaskExecutor.create("test-capacity-check", 2, 2, 2)
        executorsToClean.add(executor)

        assertTrue("Should have capacity initially", executor.hasCapacity())

        val blockingLatch = CountDownLatch(1)

        // Fill all capacity
        executor.execute(Runnable {
            try {
                blockingLatch.await(5, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        })

        executor.execute(Runnable {
            try {
                blockingLatch.await(5, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        })

        // Fill queue
        executor.execute(Runnable { })
        executor.execute(Runnable { })

        // Should have no capacity now
        assertFalse("Should have no capacity when full", executor.hasCapacity())

        // Release tasks
        blockingLatch.countDown()

        // Wait for capacity to free up
        Thread.sleep(100)

        assertTrue("Should have capacity after tasks complete", executor.hasCapacity())
    }

    @Test
    fun testExecutorStats() {
        val executor = SecureTaskExecutor.create("test-stats", 2, 4, 10)
        executorsToClean.add(executor)

        val stats = executor.getStats()

        assertNotNull("Stats should not be null", stats)
        assertEquals("Name should match", "test-stats", stats["name"])
        assertNotNull("Should have completedTasks stat", stats["completedTasks"])
        assertNotNull("Should have rejectedTasks stat", stats["rejectedTasks"])
        assertNotNull("Should have activeCount stat", stats["activeCount"])
        assertNotNull("Should have poolSize stat", stats["poolSize"])
    }

    @Test
    fun testSecureLimits() {
        // Test that security limits are enforced
        val executor = SecureTaskExecutor.create(
            "test-limits",
            corePoolSize = 1000, // Should be clamped
            maximumPoolSize = 1000, // Should be clamped  
            queueCapacity = 50000 // Should be clamped
        )
        executorsToClean.add(executor)

        // Verify limits are enforced
        assertEquals("Core pool size should be limited", 50, executor.getCorePoolSize())
        assertEquals("Max pool size should be limited", 200, executor.getMaximumPoolSize())

        val stats = executor.getStats()
        val maxPoolSize = stats["maximumPoolSize"] as Int
        assertTrue("Maximum pool size should be within security limits", maxPoolSize <= 200)
    }

    @Test
    fun testProperShutdown() {
        val executor = SecureTaskExecutor.create("test-shutdown", 2, 4, 10)
        
        val latch = CountDownLatch(1)
        
        // Execute a task
        executor.execute(Runnable {
            latch.countDown()
        })
        
        assertTrue("Task should complete", latch.await(5, TimeUnit.SECONDS))
        
        // Close executor
        executor.close()
        
        // Try to execute after shutdown - should fail
        val success = executor.execute(Runnable {
            fail("Task should not execute after shutdown")
        })
        
        assertFalse("Task should be rejected after shutdown", success)
    }

    @Test
    fun testExceptionHandling() {
        val executor = SecureTaskExecutor.create("test-exceptions", 2, 4, 10)
        executorsToClean.add(executor)

        val latch = CountDownLatch(1)
        val exceptionThrown = AtomicBoolean(false)

        // Execute task that throws exception
        val success = executor.execute(Runnable {
            exceptionThrown.set(true)
            latch.countDown()
            throw RuntimeException("Test exception")
        })

        assertTrue("Task should be accepted", success)
        assertTrue("Task should complete", latch.await(5, TimeUnit.SECONDS))
        assertTrue("Exception should have been thrown", exceptionThrown.get())

        // Executor should still be functional after exception
        val latch2 = CountDownLatch(1)
        val secondTaskExecuted = AtomicBoolean(false)

        val success2 = executor.execute(Runnable {
            secondTaskExecuted.set(true)
            latch2.countDown()
        })

        assertTrue("Second task should be accepted", success2)
        assertTrue("Second task should complete", latch2.await(5, TimeUnit.SECONDS))
        assertTrue("Second task should execute", secondTaskExecuted.get())
    }
}