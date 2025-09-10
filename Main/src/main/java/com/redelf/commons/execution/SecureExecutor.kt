package com.redelf.commons.execution

import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import kotlinx.coroutines.*
import kotlinx.coroutines.CancellationException as CoroutineCancellationException
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Secure replacement for the Executor enum with proper resource management.
 * 
 * SECURITY IMPROVEMENTS:
 * - Proper executor shutdown mechanisms
 * - Bounded coroutine scopes instead of GlobalScope
 * - Timeout protections on all operations
 * - Resource cleanup and lifecycle management
 * - Thread-safe capacity checking
 * - Comprehensive error handling
 */
class SecureExecutor private constructor(
    private val name: String,
    private val corePoolSize: Int,
    private val maximumPoolSize: Int,
    private val queueCapacity: Int
) : AutoCloseable {

    companion object {
        // Security limits
        private const val MAX_POOL_SIZE = 100
        private const val MAX_QUEUE_CAPACITY = 5000
        private const val DEFAULT_TIMEOUT_MS = 30000L
        
        // Global metrics
        private val totalExecutors = AtomicLong(0)
        private val activeExecutors = AtomicLong(0)
        
        // Pre-configured secure executors
        @JvmStatic
        public val MAIN: SecureExecutor = SecureExecutor("MAIN", 4, 20, 1000)
        
        @JvmStatic
        public val SINGLE: SecureExecutor = SecureExecutor("SINGLE", 1, 1, 100)
        
        @JvmStatic
        public val IO: SecureExecutor = SecureExecutor("IO", 8, 50, 2000)
        
        /**
         * Public static getter methods for Java compatibility
         */
        @JvmStatic
        public fun getMainExecutor(): SecureExecutor = MAIN
        
        @JvmStatic
        public fun getSingleExecutor(): SecureExecutor = SINGLE
        
        @JvmStatic
        public fun getIoExecutor(): SecureExecutor = IO
        
        /**
         * Java-friendly execute method that accepts Runnable lambda expressions
         */
        @JvmStatic
        public fun executeMain(runnable: Runnable) {
            MAIN.execute(runnable)
        }
        
        @JvmStatic
        public fun executeSingle(runnable: Runnable) {
            SINGLE.execute(runnable)
        }
        
        @JvmStatic
        public fun executeIO(runnable: Runnable) {
            IO.execute(runnable)
        }
        
        /**
         * Create a custom secure executor
         */
        fun create(
            name: String,
            corePoolSize: Int = 2,
            maximumPoolSize: Int = 10,
            queueCapacity: Int = 500
        ): SecureExecutor {
            val safeCorePoolSize = corePoolSize.coerceIn(1, MAX_POOL_SIZE)
            val safeMaximumPoolSize = maximumPoolSize.coerceIn(safeCorePoolSize, MAX_POOL_SIZE)
            val safeQueueCapacity = queueCapacity.coerceIn(1, MAX_QUEUE_CAPACITY)
            
            return SecureExecutor(name, safeCorePoolSize, safeMaximumPoolSize, safeQueueCapacity)
        }
        
        /**
         * Shutdown all executors - call on application termination
         */
        fun shutdownAll() {
            MAIN.close()
            SINGLE.close()
            IO.close()
        }
        
        fun getGlobalMetrics(): Map<String, Long> {
            return mapOf(
                "totalExecutors" to totalExecutors.get(),
                "activeExecutors" to activeExecutors.get()
            )
        }
    }

    private val tag = "SecureExecutor :: $name ::"
    private val lock = ReentrantReadWriteLock()
    
    // Thread pool management
    private val taskExecutor = SecureTaskExecutor.create(
        name = name,
        corePoolSize = corePoolSize,
        maximumPoolSize = maximumPoolSize,
        queueCapacity = queueCapacity,
        rejectionPolicy = SecureTaskExecutor.RejectionPolicy.CALLER_RUNS
    )
    
    // Bounded coroutine scope instead of GlobalScope
    private val coroutineScope = CoroutineScope(
        Dispatchers.Default + 
        SupervisorJob() + 
        CoroutineName("SecureExecutor-$name")
    )
    
    // Performance metrics
    private val executedTasks = AtomicLong(0)
    private val failedTasks = AtomicLong(0)
    private val timedOutTasks = AtomicLong(0)
    
    @Volatile
    private var isShutdown = AtomicBoolean(false)
    
    init {
        totalExecutors.incrementAndGet()
        activeExecutors.incrementAndGet()
    }

    /**
     * Execute a runnable with timeout protection
     */
    fun execute(
        action: Runnable,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MS,
        onRejected: ((Throwable) -> Unit)? = null
    ) {
        if (isShutdown.get()) {
            Console.error("$tag Cannot execute - executor is shut down")
            onRejected?.invoke(IllegalStateException("Executor is shut down"))
            return
        }

        lock.read {
            try {
                val success = taskExecutor.execute(action, timeoutMillis)
                
                if (success) {
                    executedTasks.incrementAndGet()
                } else {
                    failedTasks.incrementAndGet()
                    onRejected?.invoke(RejectedExecutionException("Task execution failed"))
                }
                
            } catch (e: Throwable) {
                failedTasks.incrementAndGet()
                Console.error("$tag Execute error: ${e.message}")
                recordException(e)
                onRejected?.invoke(e)
            }
        }
    }

    /**
     * Execute a runnable with delay (using scheduled execution instead of Thread.sleep)
     */
    fun execute(
        action: Runnable,
        delayInMillis: Long,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MS
    ) {
        if (isShutdown.get()) {
            Console.error("$tag Cannot execute delayed task - executor is shut down")
            return
        }

        // Use coroutine for proper delayed execution instead of blocking threads
        coroutineScope.launch {
            try {
                delay(delayInMillis)
                
                // Execute the action in thread pool
                val success = taskExecutor.execute(action, timeoutMillis)
                
                if (success) {
                    executedTasks.incrementAndGet()
                } else {
                    failedTasks.incrementAndGet()
                }
                
            } catch (e: CoroutineCancellationException) {
                Console.log("$tag Delayed task cancelled")
            } catch (e: Throwable) {
                failedTasks.incrementAndGet()
                Console.error("$tag Delayed execute error: ${e.message}")
                recordException(e)
            }
        }
    }

    /**
     * Execute a callable with timeout protection
     */
    fun <T> execute(
        callable: Callable<T>,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MS
    ): T? {
        if (isShutdown.get()) {
            Console.error("$tag Cannot execute callable - executor is shut down")
            return null
        }

        return lock.read {
            try {
                val result = taskExecutor.execute(callable, timeoutMillis)
                
                if (result != null) {
                    executedTasks.incrementAndGet()
                } else {
                    failedTasks.incrementAndGet()
                }
                
                result
                
            } catch (e: Throwable) {
                failedTasks.incrementAndGet()
                Console.error("$tag Execute callable error: ${e.message}")
                recordException(e)
                null
            }
        }
    }

    /**
     * Execute with result callback (async pattern)
     */
    fun <T> execute(
        callable: Callable<T>,
        callback: ResultCallback<T>,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MS
    ) {
        if (isShutdown.get()) {
            Console.error("$tag Cannot execute with callback - executor is shut down")
            callback.onFailure(IllegalStateException("Executor is shut down"))
            return
        }

        // Use coroutine scope for proper async execution
        coroutineScope.launch {
            try {
                val result = withTimeout(timeoutMillis) {
                    // Execute in thread pool
                    taskExecutor.execute(callable, timeoutMillis)
                }
                
                if (result != null) {
                    executedTasks.incrementAndGet()
                    callback.onSuccess(result)
                } else {
                    failedTasks.incrementAndGet()
                    callback.onFailure(RuntimeException("Task execution failed"))
                }
                
            } catch (e: TimeoutCancellationException) {
                timedOutTasks.incrementAndGet()
                Console.error("$tag Task timed out after ${timeoutMillis}ms")
                callback.onFailure(TimeoutException("Task timed out"))
            } catch (e: CoroutineCancellationException) {
                Console.log("$tag Task cancelled")
                callback.onFailure(e)
            } catch (e: Throwable) {
                failedTasks.incrementAndGet()
                Console.error("$tag Execute with callback error: ${e.message}")
                recordException(e)
                callback.onFailure(e)
            }
        }
    }

    /**
     * Thread-safe capacity checking
     */
    fun hasCapacity(): Boolean {
        return lock.read {
            !isShutdown.get() && taskExecutor.hasCapacity()
        }
    }

    /**
     * Get current executor statistics
     */
    fun getStats(): Map<String, Any> {
        return lock.read {
            val baseStats = taskExecutor.getStats()
            baseStats + mapOf(
                "name" to name,
                "executedTasks" to executedTasks.get(),
                "failedTasks" to failedTasks.get(),
                "timedOutTasks" to timedOutTasks.get(),
                "isShutdown" to isShutdown.get(),
                "coroutineScope.isActive" to coroutineScope.isActive
            )
        }
    }

    /**
     * Toggle thread pooled execution (backward compatibility)
     */
    fun toggleThreadPooledExecution(enabled: Boolean) {
        // This is for backward compatibility - secure executor always uses thread pools
        Console.log("$tag Thread pooled execution is always enabled in secure executor")
    }

    /**
     * Check if using thread pooled execution (backward compatibility)
     */
    fun isThreadPooledExecution(): Boolean {
        return true // Always true for secure executor
    }

    /**
     * Proper shutdown with resource cleanup
     */
    override fun close() {
        lock.write {
            if (isShutdown.compareAndSet(false, true)) {
                Console.log("$tag Starting shutdown")
                
                try {
                    // Cancel all coroutines
                    coroutineScope.cancel("SecureExecutor shutdown")
                    
                    // Wait for coroutines to complete
                    runBlocking {
                        try {
                            withTimeout(5000L) {
                                coroutineScope.coroutineContext[Job]?.join()
                            }
                        } catch (e: TimeoutCancellationException) {
                            Console.warning("$tag Coroutines did not complete within timeout")
                        }
                    }
                    
                    // Close the underlying task executor
                    taskExecutor.close()
                    
                    activeExecutors.decrementAndGet()
                    Console.log("$tag Shutdown completed successfully")
                    
                } catch (e: Throwable) {
                    Console.error("$tag Shutdown error: ${e.message}")
                    recordException(e)
                }
            } else {
                Console.warning("$tag Already shut down")
            }
        }
    }

    // Backward compatibility interface methods
    fun isDebugOn(): Boolean = false
    fun toggleDebug(enabled: Boolean) {
        Console.log("$tag Debug toggle not needed in secure executor - use logging configuration")
    }
}