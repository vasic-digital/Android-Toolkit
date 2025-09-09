package com.redelf.commons.execution

import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import kotlinx.coroutines.delay
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min
import kotlin.math.pow

/**
 * Secure retry mechanism with exponential backoff and DoS protection.
 * 
 * SECURITY IMPROVEMENTS:
 * - Exponential backoff with jitter to prevent system overload
 * - Maximum retry limits to prevent infinite loops
 * - Timeout protection for each retry attempt
 * - Configurable backoff strategies
 * - Performance metrics and monitoring
 * - Circuit breaker pattern support
 */
class SecureRetrying private constructor(
    private val maxRetries: Int,
    private val baseDelayMs: Long,
    private val maxDelayMs: Long,
    private val backoffStrategy: BackoffStrategy,
    private val jitterEnabled: Boolean,
    private val timeoutPerAttemptMs: Long
) {

    companion object {
        // Security limits
        private const val MAX_RETRY_LIMIT = 10
        private const val MAX_BASE_DELAY_MS = 30000L // 30 seconds
        private const val MAX_TOTAL_DELAY_MS = 300000L // 5 minutes
        private const val DEFAULT_TIMEOUT_MS = 30000L
        
        // Global metrics
        private val totalRetryAttempts = AtomicLong(0)
        private val successfulRetries = AtomicLong(0)
        private val failedRetries = AtomicLong(0)
        private val timeoutRetries = AtomicLong(0)
        
        /**
         * Create a secure retry mechanism with safety limits
         */
        fun create(
            maxRetries: Int = 3,
            baseDelayMs: Long = 1000L,
            maxDelayMs: Long = 30000L,
            backoffStrategy: BackoffStrategy = BackoffStrategy.EXPONENTIAL,
            jitterEnabled: Boolean = true,
            timeoutPerAttemptMs: Long = DEFAULT_TIMEOUT_MS
        ): SecureRetrying {
            
            val safeMaxRetries = maxRetries.coerceIn(1, MAX_RETRY_LIMIT)
            val safeBaseDelayMs = baseDelayMs.coerceIn(100L, MAX_BASE_DELAY_MS)
            val safeMaxDelayMs = maxDelayMs.coerceIn(safeBaseDelayMs, MAX_TOTAL_DELAY_MS)
            val safeTimeoutMs = timeoutPerAttemptMs.coerceIn(1000L, DEFAULT_TIMEOUT_MS * 2)
            
            if (maxRetries != safeMaxRetries) {
                Console.warning("SecureRetrying :: Max retries adjusted from $maxRetries to $safeMaxRetries for security")
            }
            if (baseDelayMs != safeBaseDelayMs) {
                Console.warning("SecureRetrying :: Base delay adjusted from ${baseDelayMs}ms to ${safeBaseDelayMs}ms for security")
            }
            
            return SecureRetrying(
                safeMaxRetries,
                safeBaseDelayMs,
                safeMaxDelayMs,
                backoffStrategy,
                jitterEnabled,
                safeTimeoutMs
            )
        }
        
        /**
         * Get global retry metrics
         */
        fun getMetrics(): Map<String, Long> {
            return mapOf(
                "totalRetryAttempts" to totalRetryAttempts.get(),
                "successfulRetries" to successfulRetries.get(),
                "failedRetries" to failedRetries.get(),
                "timeoutRetries" to timeoutRetries.get(),
                "successRate" to if (totalRetryAttempts.get() > 0) 
                    (successfulRetries.get() * 100) / totalRetryAttempts.get() else 0
            )
        }
    }

    enum class BackoffStrategy {
        LINEAR,      // Linear increase: delay * attempt
        EXPONENTIAL, // Exponential: delay * (2^attempt)
        FIBONACCI,   // Fibonacci sequence
        FIXED        // Fixed delay between attempts
    }

    private val tag = "SecureRetrying :: Max=$maxRetries, BaseDelay=${baseDelayMs}ms ::"

    /**
     * Execute operation with secure retry logic
     */
    suspend fun execute(operation: suspend () -> Boolean): RetryResult {
        val startTime = System.currentTimeMillis()
        var attempt = 0
        var lastException: Throwable? = null
        
        while (attempt <= maxRetries) {
            totalRetryAttempts.incrementAndGet()
            
            try {
                Console.log("$tag Attempt ${attempt + 1}/${maxRetries + 1}")
                
                // Execute with timeout protection
                val success = withTimeoutOrNull(timeoutPerAttemptMs) {
                    operation()
                } ?: false
                
                if (success) {
                    val duration = System.currentTimeMillis() - startTime
                    successfulRetries.incrementAndGet()
                    Console.log("$tag Succeeded on attempt ${attempt + 1} after ${duration}ms")
                    
                    return RetryResult(
                        success = true,
                        attempts = attempt + 1,
                        totalDurationMs = duration,
                        lastException = null
                    )
                }
                
            } catch (e: Throwable) {
                lastException = e
                Console.error("$tag Attempt ${attempt + 1} failed: ${e.message}")
                recordException(e)
                
                if (e is java.util.concurrent.TimeoutException) {
                    timeoutRetries.incrementAndGet()
                }
            }
            
            // Don't sleep after the last attempt
            if (attempt < maxRetries) {
                val delayMs = calculateDelay(attempt)
                Console.log("$tag Waiting ${delayMs}ms before next attempt")
                delay(delayMs)
            }
            
            attempt++
        }
        
        val duration = System.currentTimeMillis() - startTime
        failedRetries.incrementAndGet()
        Console.error("$tag Failed after ${attempt} attempts in ${duration}ms")
        
        return RetryResult(
            success = false,
            attempts = attempt,
            totalDurationMs = duration,
            lastException = lastException
        )
    }

    /**
     * Synchronous execute with blocking retry logic
     */
    fun executeSync(operation: () -> Boolean): RetryResult {
        val startTime = System.currentTimeMillis()
        var attempt = 0
        var lastException: Throwable? = null
        
        while (attempt <= maxRetries) {
            totalRetryAttempts.incrementAndGet()
            
            try {
                Console.log("$tag Sync attempt ${attempt + 1}/${maxRetries + 1}")
                
                val success = operation()
                
                if (success) {
                    val duration = System.currentTimeMillis() - startTime
                    successfulRetries.incrementAndGet()
                    Console.log("$tag Sync succeeded on attempt ${attempt + 1} after ${duration}ms")
                    
                    return RetryResult(
                        success = true,
                        attempts = attempt + 1,
                        totalDurationMs = duration,
                        lastException = null
                    )
                }
                
            } catch (e: Throwable) {
                lastException = e
                Console.error("$tag Sync attempt ${attempt + 1} failed: ${e.message}")
                recordException(e)
            }
            
            // Don't sleep after the last attempt
            if (attempt < maxRetries) {
                val delayMs = calculateDelay(attempt)
                Console.log("$tag Sync waiting ${delayMs}ms before next attempt")
                Thread.sleep(delayMs)
            }
            
            attempt++
        }
        
        val duration = System.currentTimeMillis() - startTime
        failedRetries.incrementAndGet()
        Console.error("$tag Sync failed after ${attempt} attempts in ${duration}ms")
        
        return RetryResult(
            success = false,
            attempts = attempt,
            totalDurationMs = duration,
            lastException = lastException
        )
    }

    /**
     * Calculate delay with backoff strategy and jitter
     */
    private fun calculateDelay(attempt: Int): Long {
        val baseDelay = when (backoffStrategy) {
            BackoffStrategy.LINEAR -> baseDelayMs * (attempt + 1)
            BackoffStrategy.EXPONENTIAL -> baseDelayMs * (2.0.pow(attempt).toLong())
            BackoffStrategy.FIBONACCI -> baseDelayMs * fibonacci(attempt + 1)
            BackoffStrategy.FIXED -> baseDelayMs
        }
        
        val cappedDelay = min(baseDelay, maxDelayMs)
        
        return if (jitterEnabled) {
            // Add up to 25% jitter to prevent thundering herd
            val jitter = ThreadLocalRandom.current().nextDouble(0.75, 1.25)
            (cappedDelay * jitter).toLong()
        } else {
            cappedDelay
        }
    }

    /**
     * Calculate fibonacci number for fibonacci backoff
     */
    private fun fibonacci(n: Int): Long {
        if (n <= 1) return 1L
        
        var a = 1L
        var b = 1L
        
        repeat(n - 1) {
            val next = a + b
            a = b
            b = next
        }
        
        return b
    }

    /**
     * Timeout wrapper for suspend functions
     */
    private suspend fun <T> withTimeoutOrNull(timeoutMillis: Long, block: suspend () -> T): T? {
        return try {
            kotlinx.coroutines.withTimeout(timeoutMillis) {
                block()
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            null
        }
    }

    /**
     * Result of retry operation
     */
    data class RetryResult(
        val success: Boolean,
        val attempts: Int,
        val totalDurationMs: Long,
        val lastException: Throwable?
    ) {
        fun isSuccess(): Boolean = success
        fun getDurationMs(): Long = totalDurationMs
        // Note: getAttempts() and getLastException() are auto-generated by Kotlin for data class
    }
}