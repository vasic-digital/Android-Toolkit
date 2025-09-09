package com.redelf.commons.execution

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * @deprecated This class has critical security vulnerabilities:
 * - Unbounded queues that can cause DoS attacks and memory exhaustion
 * - No rejection policy defined (defaults to AbortPolicy)
 * - Maximum pool size can create thousands of threads (capacity * 100)
 * - No timeout protections or resource management
 * - No executor shutdown mechanism causing memory leaks
 * 
 * Use SecureTaskExecutor instead which provides:
 * - Bounded queues with configurable limits
 * - Proper rejection policies and error handling
 * - Timeout protections on all operations
 * - Automatic resource cleanup and shutdown
 * - Performance monitoring and metrics
 * 
 * @see SecureTaskExecutor
 */
@Deprecated(
    message = "Critical security vulnerabilities: unbounded queues, DoS potential, memory leaks. Use SecureTaskExecutor instead.",
    replaceWith = ReplaceWith("SecureTaskExecutor.create(name, corePoolSize, maximumPoolSize, queueCapacity)", "com.redelf.commons.execution.SecureTaskExecutor"),
    level = DeprecationLevel.WARNING
)
class TaskExecutor private constructor(
    private val secureExecutor: SecureTaskExecutor
) : ThreadPoolExecutor(
    secureExecutor.getCorePoolSize(),
    secureExecutor.getMaximumPoolSize(),
    0L,
    TimeUnit.MILLISECONDS,
    LinkedBlockingQueue(1) // Dummy queue since we delegate
) {
    
    // Delegate all operations to secure executor
    override fun execute(command: Runnable) {
        secureExecutor.execute(command)
    }
    
    override fun shutdown() {
        secureExecutor.close()
        super.shutdown()
    }
    
    override fun shutdownNow(): MutableList<Runnable> {
        secureExecutor.close()
        return super.shutdownNow()
    }

    companion object {

        fun instantiate(capacity: Int): ThreadPoolExecutor {
            // Create secure executor with safe limits instead of capacity * 100
            val safeMaxPoolSize = (capacity * 5).coerceAtMost(50)
            val secureExecutor = SecureTaskExecutor.create(
                name = "TaskExecutor-Main",
                corePoolSize = capacity,
                maximumPoolSize = safeMaxPoolSize,
                queueCapacity = (capacity * 10).coerceAtMost(1000)
            )
            
            return TaskExecutor(secureExecutor)
        }

        fun instantiateSingle(): ThreadPoolExecutor {
            val secureExecutor = SecureTaskExecutor.create(
                name = "TaskExecutor-Single",
                corePoolSize = 1,
                maximumPoolSize = 1,
                queueCapacity = 100
            )
            
            return TaskExecutor(secureExecutor)
        }
    }
}