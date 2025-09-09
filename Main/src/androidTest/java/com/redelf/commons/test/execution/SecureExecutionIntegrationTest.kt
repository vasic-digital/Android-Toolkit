package com.redelf.commons.test.execution

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.redelf.commons.execution.*
import com.redelf.commons.test.BaseTest
import kotlinx.coroutines.runBlocking
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
class SecureExecutionIntegrationTest : BaseTest() {

    private val executorsToClean = mutableListOf<SecureExecutor>()
    private val taskExecutorsToClean = mutableListOf<SecureTaskExecutor>()

    @After
    fun cleanup() {
        executorsToClean.forEach { it.close() }
        executorsToClean.clear()
        taskExecutorsToClean.forEach { it.close() }
        taskExecutorsToClean.clear()
    }

    @Test
    fun testSecureExecutorWithRetrying() {
        val executor = SecureExecutor.create("integration-test", 2, 4, 10)
        executorsToClean.add(executor)

        val retrying = SecureRetrying.create(
            maxRetries = 3,
            baseDelayMs = 50
        )

        val attempts = AtomicInteger(0)
        val latch = CountDownLatch(1)
        val finalResult = AtomicBoolean(false)

        // Use executor with retrying logic
        executor.execute(Runnable {
            runBlocking {
                val result = retrying.execute {
                    val attempt = attempts.incrementAndGet()
                    attempt >= 2 // Succeed on 2nd attempt
                }
                
                finalResult.set(result.success)
                latch.countDown()
            }
        })

        assertTrue("Integration test should complete", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Retrying should succeed", finalResult.get())
        assertEquals("Should retry once", 2, attempts.get())
    }

    @Test
    fun testBackwardCompatibilityWithOriginalExecutor() {
        // Test that the deprecated Executor still works through delegation
        val latch = CountDownLatch(3)
        val results = AtomicInteger(0)

        // Test MAIN executor
        val success1 = Executor.MAIN.execute(Runnable {
            results.incrementAndGet()
            latch.countDown()
        })

        // Test delayed execution
        Executor.MAIN.execute(Runnable {
            results.incrementAndGet()
            latch.countDown()
        }, 50)

        // Test callable execution
        val callable = Callable {
            results.incrementAndGet()
            latch.countDown()
            "test-result"
        }
        val callableResult = Executor.MAIN.execute(callable)

        assertTrue("MAIN executor should accept tasks", success1)
        assertEquals("Callable should return result", "test-result", callableResult)
        assertTrue("All tasks should complete", latch.await(5, TimeUnit.SECONDS))
        assertEquals("All tasks should execute", 3, results.get())
    }

    @Test
    fun testTaskExecutorDelegation() {
        // Test that deprecated TaskExecutor delegates to SecureTaskExecutor
        val taskExecutor = TaskExecutor.instantiate(5)
        
        val latch = CountDownLatch(1)
        val executed = AtomicBoolean(false)

        taskExecutor.execute(Runnable {
            executed.set(true)
            latch.countDown()
        })

        assertTrue("Task should complete", latch.await(5, TimeUnit.SECONDS))
        assertTrue("Task should execute", executed.get())
        
        // Cleanup
        taskExecutor.shutdown()
    }

    @Test
    fun testHighVolumeExecution() {
        val executor = SecureExecutor.create("high-volume", 4, 8, 100)
        executorsToClean.add(executor)

        val taskCount = 200
        val latch = CountDownLatch(taskCount)
        val completedTasks = AtomicInteger(0)
        val errors = AtomicInteger(0)

        repeat(taskCount) { taskId ->
            executor.execute(Runnable {
                try {
                    // Simulate some work
                    Thread.sleep(1)
                    completedTasks.incrementAndGet()
                } catch (e: Exception) {
                    errors.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            })
        }

        assertTrue("All high volume tasks should complete", 
                  latch.await(30, TimeUnit.SECONDS))
        assertTrue("Most tasks should complete successfully", 
                  completedTasks.get() > taskCount * 0.95)
        assertEquals("Should have minimal errors", 0, errors.get())
    }

    @Test
    fun testSecureExecutorStats() {
        val executor = SecureExecutor.create("stats-test", 2, 4, 20)
        executorsToClean.add(executor)

        val initialStats = executor.getStats()
        assertNotNull("Stats should not be null", initialStats)

        val taskCount = 10
        val latch = CountDownLatch(taskCount)

        // Execute some tasks
        repeat(taskCount) {
            executor.execute(Runnable {
                Thread.sleep(10)
                latch.countDown()
            })
        }

        assertTrue("Tasks should complete", latch.await(10, TimeUnit.SECONDS))

        val finalStats = executor.getStats()
        val executedTasks = finalStats["executedTasks"] as Long
        assertTrue("Should have executed tasks", executedTasks >= taskCount)
    }

    @Test
    fun testRetryingWithDifferentStrategies() = runBlocking {
        val strategies = listOf(
            SecureRetrying.BackoffStrategy.LINEAR,
            SecureRetrying.BackoffStrategy.EXPONENTIAL,
            SecureRetrying.BackoffStrategy.FIBONACCI,
            SecureRetrying.BackoffStrategy.FIXED
        )

        strategies.forEach { strategy ->
            val retrying = SecureRetrying.create(
                maxRetries = 3,
                baseDelayMs = 10,
                backoffStrategy = strategy,
                jitterEnabled = false
            )

            val attempts = AtomicInteger(0)
            val result = retrying.execute {
                val attempt = attempts.incrementAndGet()
                attempt >= 2 // Succeed on 2nd attempt
            }

            assertTrue("Strategy $strategy should succeed", result.success)
            assertEquals("Strategy $strategy should attempt twice", 2, result.attempts)
        }
    }

    @Test
    fun testErrorRecoveryAndResilience() {
        val executor = SecureExecutor.create("error-recovery", 2, 4, 20)
        executorsToClean.add(executor)

        val successfulTasks = AtomicInteger(0)
        val failedTasks = AtomicInteger(0)
        val totalTasks = 50
        val latch = CountDownLatch(totalTasks)

        repeat(totalTasks) { taskId ->
            executor.execute(Runnable {
                try {
                    if (taskId % 5 == 0) {
                        // Every 5th task throws an exception
                        throw RuntimeException("Simulated failure $taskId")
                    } else {
                        // Normal successful task
                        successfulTasks.incrementAndGet()
                    }
                } catch (e: Exception) {
                    failedTasks.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            })
        }

        assertTrue("All tasks should complete", latch.await(15, TimeUnit.SECONDS))
        assertTrue("Should have successful tasks", successfulTasks.get() > 0)
        assertTrue("Should have some failed tasks", failedTasks.get() > 0)
        assertEquals("All tasks should be accounted for", 
                    totalTasks, successfulTasks.get() + failedTasks.get())

        // Executor should still be functional after exceptions
        val postErrorLatch = CountDownLatch(1)
        val postErrorSuccess = AtomicBoolean(false)

        executor.execute(Runnable {
            postErrorSuccess.set(true)
            postErrorLatch.countDown()
        })

        assertTrue("Executor should work after errors", 
                  postErrorLatch.await(5, TimeUnit.SECONDS))
        assertTrue("Post-error task should succeed", postErrorSuccess.get())
    }

    @Test
    fun testResourceManagementAndCleanup() {
        val executor = SecureExecutor.create("resource-test", 2, 4, 10)
        
        val taskCount = 20
        val latch = CountDownLatch(taskCount)
        val completedTasks = AtomicInteger(0)

        // Execute tasks
        repeat(taskCount) {
            executor.execute(Runnable {
                completedTasks.incrementAndGet()
                latch.countDown()
            })
        }

        assertTrue("Tasks should complete before shutdown", 
                  latch.await(10, TimeUnit.SECONDS))
        assertEquals("All tasks should complete", taskCount, completedTasks.get())

        // Test graceful shutdown
        executor.close()

        // Try to execute after shutdown
        val postShutdownSuccess = executor.execute(Runnable {
            fail("Should not execute after shutdown")
        })

        assertFalse("Should reject tasks after shutdown", postShutdownSuccess)
    }

    @Test
    fun testConcurrentAccessToExecutors() {
        val executor = SecureExecutor.create("concurrent-test", 4, 8, 50)
        executorsToClean.add(executor)

        val threadCount = 10
        val tasksPerThread = 20
        val totalTasks = threadCount * tasksPerThread
        val latch = CountDownLatch(totalTasks)
        val completedTasks = AtomicInteger(0)
        val threads = mutableListOf<Thread>()

        repeat(threadCount) { threadId ->
            val thread = Thread {
                repeat(tasksPerThread) { taskId ->
                    executor.execute(Runnable {
                        // Simulate work with thread ID for uniqueness
                        val threadName = Thread.currentThread().name
                        Thread.sleep(1)
                        completedTasks.incrementAndGet()
                        latch.countDown()
                    })
                }
            }
            threads.add(thread)
            thread.start()
        }

        // Wait for all threads to complete
        threads.forEach { it.join(10000) }

        assertTrue("All concurrent tasks should complete", 
                  latch.await(20, TimeUnit.SECONDS))
        assertEquals("All tasks should execute", totalTasks, completedTasks.get())
    }

    @Test
    fun testExecutorWithCallbackPattern() {
        val executor = SecureExecutor.create("callback-test", 2, 4, 10)
        executorsToClean.add(executor)

        val latch = CountDownLatch(1)
        val result = AtomicReference<String>()
        val error = AtomicReference<Throwable>()

        val callable = Callable {
            Thread.sleep(50)
            "callback-result"
        }

        val callback = object : ResultCallback<String> {
            override fun onSuccess(callbackResult: String) {
                result.set(callbackResult)
                latch.countDown()
            }

            override fun onFailure(throwable: Throwable) {
                error.set(throwable)
                latch.countDown()
            }
        }

        executor.execute(callable, callback)

        assertTrue("Callback should be invoked", latch.await(5, TimeUnit.SECONDS))
        assertEquals("Should receive correct result", "callback-result", result.get())
        assertNull("Should have no error", error.get())
    }

    @Test
    fun testExecutorCapacityManagement() {
        val executor = SecureExecutor.create("capacity-test", 2, 2, 2)
        executorsToClean.add(executor)

        assertTrue("Should have capacity initially", executor.hasCapacity())

        val blockingLatch = CountDownLatch(1)

        // Fill all capacity
        executor.execute(Runnable {
            try {
                blockingLatch.await(10, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        })

        executor.execute(Runnable {
            try {
                blockingLatch.await(10, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        })

        // Fill queue
        executor.execute(Runnable { })
        executor.execute(Runnable { })

        // Should have no capacity now
        Thread.sleep(100) // Give time for tasks to be queued
        assertFalse("Should have no capacity when full", executor.hasCapacity())

        // Release blocking tasks
        blockingLatch.countDown()

        // Wait for capacity to return
        Thread.sleep(200)
        assertTrue("Should have capacity after completion", executor.hasCapacity())
    }
}