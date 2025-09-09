package com.redelf.commons.execution

import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Secure replacement for TaskExecutor with comprehensive safety measures.
 * 
 * SECURITY IMPROVEMENTS:
 * - Bounded queues to prevent DoS attacks
 * - Configurable rejection policies
 * - Proper executor shutdown mechanisms
 * - Resource management and cleanup
 * - Timeout protections
 * - Performance monitoring
 */
class SecureTaskExecutor private constructor(
    private val corePoolSize: Int,
    private val maximumPoolSize: Int,
    private val queueCapacity: Int,
    private val rejectionPolicy: RejectedExecutionHandler
) : AutoCloseable {

    companion object {
        // Security limits
        private const val MAX_CORE_POOL_SIZE = 50
        private const val MAX_MAXIMUM_POOL_SIZE = 200
        private const val MAX_QUEUE_CAPACITY = 10000
        private const val DEFAULT_KEEP_ALIVE_TIME = 60L
        private const val SHUTDOWN_TIMEOUT_SECONDS = 30L
        
        // Performance metrics
        private val totalExecutors = AtomicLong(0)
        private val activeExecutors = AtomicLong(0)
        
        /**
         * Create a secure task executor with safety limits
         */
        fun create(
            name: String,
            corePoolSize: Int = 2,
            maximumPoolSize: Int = 10,
            queueCapacity: Int = 1000,
            rejectionPolicy: RejectionPolicy = RejectionPolicy.CALLER_RUNS
        ): SecureTaskExecutor {
            // Validate and enforce security limits
            val safeCorePoolSize = corePoolSize.coerceIn(1, MAX_CORE_POOL_SIZE)
            val safeMaximumPoolSize = maximumPoolSize.coerceIn(safeCorePoolSize, MAX_MAXIMUM_POOL_SIZE)
            val safeQueueCapacity = queueCapacity.coerceIn(1, MAX_QUEUE_CAPACITY)
            
            if (corePoolSize != safeCorePoolSize) {
                Console.warning("SecureTaskExecutor :: Core pool size adjusted from $corePoolSize to $safeCorePoolSize for security")
            }
            if (maximumPoolSize != safeMaximumPoolSize) {
                Console.warning("SecureTaskExecutor :: Maximum pool size adjusted from $maximumPoolSize to $safeMaximumPoolSize for security")
            }
            if (queueCapacity != safeQueueCapacity) {
                Console.warning("SecureTaskExecutor :: Queue capacity adjusted from $queueCapacity to $safeQueueCapacity for security")
            }
            
            val rejectionHandler = when (rejectionPolicy) {
                RejectionPolicy.ABORT -> ThreadPoolExecutor.AbortPolicy()
                RejectionPolicy.CALLER_RUNS -> ThreadPoolExecutor.CallerRunsPolicy()
                RejectionPolicy.DISCARD_OLDEST -> ThreadPoolExecutor.DiscardOldestPolicy()
                RejectionPolicy.DISCARD -> ThreadPoolExecutor.DiscardPolicy()
                RejectionPolicy.CUSTOM_LOG -> CustomLoggingRejectionHandler(name)
            }
            
            totalExecutors.incrementAndGet()
            activeExecutors.incrementAndGet()
            
            return SecureTaskExecutor(safeCorePoolSize, safeMaximumPoolSize, safeQueueCapacity, rejectionHandler)
        }
        
        /**
         * Get metrics for monitoring all executors
         */
        fun getMetrics(): Map<String, Long> {
            return mapOf(
                "totalExecutors" to totalExecutors.get(),
                "activeExecutors" to activeExecutors.get()
            )
        }
    }

    enum class RejectionPolicy {
        ABORT,          // Throw exception (default)
        CALLER_RUNS,    // Execute in caller thread (recommended for most cases)
        DISCARD_OLDEST, // Drop oldest task
        DISCARD,        // Drop new task
        CUSTOM_LOG      // Log and drop (custom implementation)
    }

    private val tag = "SecureTaskExecutor :: Core=$corePoolSize, Max=$maximumPoolSize, Queue=$queueCapacity ::"
    private val lock = ReentrantReadWriteLock()
    
    // Bounded queue with strict capacity
    private val workQueue = ArrayBlockingQueue<Runnable>(queueCapacity)
    
    // Thread pool with custom thread factory
    private val executor: ThreadPoolExecutor = ThreadPoolExecutor(
        corePoolSize,
        maximumPoolSize,
        DEFAULT_KEEP_ALIVE_TIME,
        TimeUnit.SECONDS,
        workQueue,
        SecureThreadFactory("SecureTaskExecutor"),
        rejectionPolicy
    ).apply {
        // Allow core threads to timeout for better resource management
        allowCoreThreadTimeOut(true)
    }

    // Performance metrics
    private val submittedTasks = AtomicLong(0)
    private val completedTasks = AtomicLong(0)
    private val rejectedTasks = AtomicLong(0)
    
    @Volatile
    private var isShutdown = false

    /**
     * Execute a task with timeout protection
     */
    fun execute(task: Runnable, timeoutMillis: Long = 30000L): Boolean {
        if (isShutdown) {
            Console.error("$tag Cannot execute task - executor is shut down")
            return false
        }

        return lock.read {
            try {
                submittedTasks.incrementAndGet()
                
                // Submit with timeout protection
                val future = executor.submit {
                    try {
                        task.run()
                        completedTasks.incrementAndGet()
                    } catch (e: Throwable) {
                        Console.error("$tag Task execution failed: ${e.message}")
                        recordException(e)
                        throw e
                    }
                }
                
                // Wait for completion with timeout
                try {
                    future.get(timeoutMillis, TimeUnit.MILLISECONDS)
                    true
                } catch (e: TimeoutException) {
                    Console.error("$tag Task timed out after ${timeoutMillis}ms")
                    future.cancel(true)
                    recordException(e)
                    false
                } catch (e: Exception) {
                    Console.error("$tag Task execution error: ${e.message}")
                    recordException(e)
                    false
                }
                
            } catch (e: RejectedExecutionException) {
                rejectedTasks.incrementAndGet()
                Console.error("$tag Task rejected: ${e.message}")
                recordException(e)
                false
            } catch (e: Throwable) {
                Console.error("$tag Unexpected error: ${e.message}")
                recordException(e)
                false
            }
        }
    }

    /**
     * Execute a callable with timeout protection
     */
    fun <T> execute(callable: Callable<T>, timeoutMillis: Long = 30000L): T? {
        if (isShutdown) {
            Console.error("$tag Cannot execute callable - executor is shut down")
            return null
        }

        return lock.read {
            try {
                submittedTasks.incrementAndGet()
                
                val future = executor.submit(callable)
                
                try {
                    val result = future.get(timeoutMillis, TimeUnit.MILLISECONDS)
                    completedTasks.incrementAndGet()
                    result
                } catch (e: TimeoutException) {
                    Console.error("$tag Callable timed out after ${timeoutMillis}ms")
                    future.cancel(true)
                    recordException(e)
                    null
                } catch (e: Exception) {
                    Console.error("$tag Callable execution error: ${e.message}")
                    recordException(e)
                    null
                }
                
            } catch (e: RejectedExecutionException) {
                rejectedTasks.incrementAndGet()
                Console.error("$tag Callable rejected: ${e.message}")
                recordException(e)
                null
            } catch (e: Throwable) {
                Console.error("$tag Unexpected error: ${e.message}")
                recordException(e)
                null
            }
        }
    }

    /**
     * Get current executor statistics
     */
    fun getStats(): Map<String, Any> {
        return lock.read {
            mapOf(
                "isShutdown" to isShutdown,
                "corePoolSize" to executor.corePoolSize,
                "maximumPoolSize" to executor.maximumPoolSize,
                "currentPoolSize" to executor.poolSize,
                "activeThreads" to executor.activeCount,
                "queueSize" to executor.queue.size,
                "queueCapacity" to queueCapacity,
                "submittedTasks" to submittedTasks.get(),
                "completedTasks" to executor.completedTaskCount,
                "rejectedTasks" to rejectedTasks.get()
            )
        }
    }

    /**
     * Check if executor has capacity for more tasks
     */
    fun hasCapacity(): Boolean {
        return lock.read {
            !isShutdown && executor.queue.remainingCapacity() > 0
        }
    }

    /**
     * Get remaining queue capacity
     */
    fun getRemainingCapacity(): Int {
        return lock.read {
            if (isShutdown) 0 else executor.queue.remainingCapacity()
        }
    }
    
    /**
     * Get core pool size (for backward compatibility)
     */
    fun getCorePoolSize(): Int = corePoolSize
    
    /**
     * Get maximum pool size (for backward compatibility)
     */
    fun getMaximumPoolSize(): Int = maximumPoolSize

    /**
     * Graceful shutdown with proper cleanup
     */
    override fun close() {
        lock.write {
            if (isShutdown) {
                Console.warning("$tag Already shut down")
                return@write
            }

            Console.log("$tag Starting shutdown")
            isShutdown = true
            
            try {
                // Graceful shutdown
                executor.shutdown()
                
                // Wait for existing tasks to complete
                val terminated = executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                
                if (!terminated) {
                    Console.warning("$tag Forcing shutdown - some tasks may be interrupted")
                    val droppedTasks = executor.shutdownNow()
                    Console.warning("$tag Dropped ${droppedTasks.size} pending tasks")
                    
                    // Wait a bit more for forced shutdown
                    executor.awaitTermination(5, TimeUnit.SECONDS)
                }
                
                activeExecutors.decrementAndGet()
                Console.log("$tag Shutdown completed successfully")
                
            } catch (e: InterruptedException) {
                Console.error("$tag Shutdown interrupted")
                executor.shutdownNow()
                Thread.currentThread().interrupt()
                recordException(e)
            } catch (e: Throwable) {
                Console.error("$tag Shutdown error: ${e.message}")
                recordException(e)
            }
        }
    }

    /**
     * Custom thread factory for better thread management
     */
    private class SecureThreadFactory(private val namePrefix: String) : ThreadFactory {
        private val threadNumber = AtomicLong(1)
        private val group = ThreadGroup("$namePrefix-ThreadGroup")

        override fun newThread(r: Runnable): Thread {
            val thread = Thread(group, r, "$namePrefix-${threadNumber.getAndIncrement()}")
            
            // Security settings
            thread.isDaemon = false  // Not daemon to ensure proper shutdown
            thread.priority = Thread.NORM_PRIORITY
            
            // Uncaught exception handler
            thread.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { t, e ->
                Console.error("$namePrefix :: Uncaught exception in thread ${t.name}: ${e.message}")
                recordException(e)
            }
            
            return thread
        }
    }

    /**
     * Custom rejection handler that logs rejected tasks
     */
    private class CustomLoggingRejectionHandler(private val executorName: String) : RejectedExecutionHandler {
        override fun rejectedExecution(r: Runnable, executor: ThreadPoolExecutor) {
            val message = "Task rejected by $executorName - Pool: ${executor.poolSize}/${executor.maximumPoolSize}, Queue: ${executor.queue.size}"
            Console.error("SecureTaskExecutor :: $message")
            recordException(RejectedExecutionException(message))
            // Task is dropped but logged
        }
    }
}