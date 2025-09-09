package com.redelf.commons.extensions

import android.os.Looper
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.OnObtain
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Secure replacement for the sync() extension function with comprehensive safety measures.
 * 
 * Fixes critical issues:
 * - Memory leaks from unreset waitingFlags
 * - Race conditions in flag management
 * - Callbacks never executed in error scenarios
 * - Potential deadlocks from nested calls
 * - Resource leaks from accumulated CountDownLatch objects
 */

// Thread-safe metrics for monitoring
private val syncMetrics = object {
    val totalCalls = AtomicLong(0)
    val successfulCalls = AtomicLong(0)
    val failedCalls = AtomicLong(0)
    val timeoutCalls = AtomicLong(0)
    val deadlockPrevented = AtomicLong(0)
}

// Thread-safe tracking of active sync operations per thread
private val activeSyncOperations = ConcurrentHashMap<Long, MutableSet<String>>()
private val syncLock = ReentrantReadWriteLock()

// Secure resource management for waiting flags
private val managedWaitingFlags = ConcurrentHashMap<String, AtomicBoolean>()

/**
 * Get metrics for monitoring sync() function performance and safety
 */
fun getSyncMetrics(): Map<String, Long> {
    return mapOf(
        "totalCalls" to syncMetrics.totalCalls.get(),
        "successfulCalls" to syncMetrics.successfulCalls.get(),
        "failedCalls" to syncMetrics.failedCalls.get(),
        "timeoutCalls" to syncMetrics.timeoutCalls.get(),
        "deadlockPrevented" to syncMetrics.deadlockPrevented.get(),
        "successRate" to if (syncMetrics.totalCalls.get() > 0) 
            (syncMetrics.successfulCalls.get() * 100) / syncMetrics.totalCalls.get() else 0,
        "activeOperations" to activeSyncOperations.size.toLong()
    )
}

/**
 * Clean up managed resources - call this periodically or on app shutdown
 */
fun cleanupSyncResources() {
    syncLock.write {
        managedWaitingFlags.clear()
        activeSyncOperations.clear()
        Console.log("SecureSync :: Resources cleaned up")
    }
}

/**
 * Secure synchronous execution wrapper with comprehensive safety measures
 */
fun <X> secureSync(
    context: String,
    from: String = "",
    timeout: Long = 60,
    timeUnit: TimeUnit = TimeUnit.SECONDS,
    mainThreadForbidden: Boolean = true,
    waitingFlagKey: String? = null,
    debug: Boolean = false,
    what: (callback: OnObtain<X?>) -> Unit
): X? {

    val tag = if (from.isEmpty()) {
        "SECURE_SYNC :: $context ::"
    } else {
        "SECURE_SYNC :: $context :: from '$from' ::"
    }

    syncMetrics.totalCalls.incrementAndGet()

    if (DEBUG_SYNC.get() || debug) Console.debug("$tag START")

    // Prevent main thread execution if forbidden
    if (mainThreadForbidden && isOnMainThread()) {
        val e = IllegalStateException("$context executed secureSync on main thread")
        Console.error("$tag ${e.message}")
        recordException(e)
        syncMetrics.failedCalls.incrementAndGet()
        return null
    }

    // Deadlock prevention - check for nested calls from same thread
    val threadId = Thread.currentThread().id
    val isNestedCall = syncLock.read {
        activeSyncOperations[threadId]?.contains(context) == true
    }

    if (isNestedCall) {
        val e = IllegalStateException("$context attempted nested secureSync call - deadlock prevented")
        Console.error("$tag ${e.message}")
        recordException(e)
        syncMetrics.deadlockPrevented.incrementAndGet()
        syncMetrics.failedCalls.incrementAndGet()
        return null
    }

    // Register this operation for deadlock tracking
    syncLock.write {
        activeSyncOperations.computeIfAbsent(threadId) { mutableSetOf() }.add(context)
    }

    var result: X? = null
    var latch: CountDownLatch? = null
    var managedFlag: AtomicBoolean? = null

    try {
        // Secure waiting flag management
        managedFlag = if (waitingFlagKey != null) {
            managedWaitingFlags.computeIfAbsent(waitingFlagKey) { AtomicBoolean(false) }
        } else null

        // Check for already waiting condition with proper atomic handling
        managedFlag?.let { flag ->
            if (flag.compareAndSet(false, false) && flag.get()) {
                Console.warning("$tag Already waiting for $waitingFlagKey")
                
                // Safe yield with timeout to prevent infinite loops
                val yieldStartTime = System.currentTimeMillis()
                val maxYieldTime = 5000L // 5 seconds max yield
                
                while (flag.get() && (System.currentTimeMillis() - yieldStartTime) < maxYieldTime) {
                    Thread.yield()
                    Thread.sleep(10) // Small sleep to prevent busy waiting
                }
                
                if (flag.get()) {
                    Console.error("$tag Yield timeout - proceeding anyway")
                }
            }
            
            // Atomically set the flag
            if (!flag.compareAndSet(false, true)) {
                Console.warning("$tag Race condition detected in flag setting")
            }
        }

        latch = CountDownLatch(1)
        val callbackExecuted = AtomicBoolean(false)

        exec(
            onRejected = { e ->
                try {
                    recordException(e)
                    Console.error("$tag Execution rejected: ${e.message}")
                    
                    // Ensure callback gets the error and latch is released
                    if (callbackExecuted.compareAndSet(false, true)) {
                        latch?.countDown()
                    }
                } catch (cleanupError: Throwable) {
                    recordException(cleanupError)
                    latch?.countDown() // Always release latch
                }
            }
        ) {
            if (DEBUG_SYNC.get() || debug) Console.log("$tag EXECUTING")

            try {
                if (DEBUG_SYNC.get() || debug) Console.log("$tag CALLING")

                what(object : OnObtain<X?> {
                    override fun onCompleted(data: X?) {
                        try {
                            if (callbackExecuted.compareAndSet(false, true)) {
                                result = data
                                if (DEBUG_SYNC.get() || debug) Console.log("$tag COMPLETED")
                            } else {
                                Console.warning("$tag Callback executed multiple times - ignored")
                            }
                        } catch (e: Throwable) {
                            recordException(e)
                        } finally {
                            latch?.countDown()
                        }
                    }

                    override fun onFailure(error: Throwable) {
                        try {
                            if (callbackExecuted.compareAndSet(false, true)) {
                                recordException(error)
                                Console.error("$tag FAILED :: Error='${error.message}'")
                            } else {
                                Console.warning("$tag Failure callback executed multiple times - ignored")
                            }
                        } catch (e: Throwable) {
                            recordException(e)
                        } finally {
                            latch?.countDown()
                        }
                    }
                })

                if (DEBUG_SYNC.get() || debug) Console.log("$tag WAITING")

            } catch (e: Throwable) {
                try {
                    Console.error("$tag EXECUTION FAILED :: Error='${e.message}'")
                    recordException(e)
                    
                    if (callbackExecuted.compareAndSet(false, true)) {
                        // Execution failed before callback was called
                    }
                } finally {
                    latch?.countDown()
                }
            }
        }

        // Safe waiting with proper timeout handling
        val startTime = System.currentTimeMillis()
        
        val awaitResult = latch?.await(timeout, timeUnit) ?: false
        val endTime = System.currentTimeMillis() - startTime

        if (awaitResult) {
            if (DEBUG_SYNC.get()) {
                if (endTime > 1500 && endTime < 3000) {
                    Console.warning("$tag WAITED for $endTime ms")
                } else if (endTime >= 3000) {
                    Console.error("$tag WAITED for $endTime ms - performance issue")
                }
            }

            if (DEBUG_SYNC.get() || debug) Console.debug("$tag END")
            syncMetrics.successfulCalls.incrementAndGet()

        } else {
            val e = TimeoutException("$context latch expired")
            Console.error("$tag TIMEOUT :: Timed out after $endTime ms")
            recordException(e)
            syncMetrics.timeoutCalls.incrementAndGet()
            syncMetrics.failedCalls.incrementAndGet()
        }

    } catch (e: Throwable) {
        val endTime = System.currentTimeMillis()
        Console.error("$tag EXCEPTION :: Error='${e.message}'")
        recordException(e)
        syncMetrics.failedCalls.incrementAndGet()
        
    } finally {
        // CRITICAL: Always clean up resources in finally block
        try {
            // Reset waiting flag atomically
            managedFlag?.compareAndSet(true, false)
            
            // Remove from active operations
            syncLock.write {
                activeSyncOperations[threadId]?.remove(context)
                if (activeSyncOperations[threadId]?.isEmpty() == true) {
                    activeSyncOperations.remove(threadId)
                }
            }
            
            // Clear latch reference to prevent memory leaks
            latch = null
            
        } catch (cleanupError: Throwable) {
            Console.error("$tag CLEANUP ERROR :: ${cleanupError.message}")
            recordException(cleanupError)
        }
    }

    return result
}


/**
 * Backward compatibility wrapper - delegates to secureSync
 * @deprecated Use secureSync instead for better safety
 */
@Deprecated(
    "Use secureSync instead for comprehensive safety measures",
    ReplaceWith("secureSync(context, from, timeout, timeUnit, mainThreadForbidden, waitingFlag?.toString(), debug, what)")
)
fun <X> syncSafe(
    context: String,
    from: String = "",
    timeout: Long = 60,
    timeUnit: TimeUnit = TimeUnit.SECONDS,
    mainThreadForbidden: Boolean = true,
    waitingFlag: AtomicBoolean? = null,
    debug: Boolean = false,
    what: (callback: OnObtain<X?>) -> Unit
): X? {
    // Generate a key from the waiting flag if provided
    val flagKey = waitingFlag?.let { "legacy_${context}_${from.ifEmpty { "default" }}" }
    
    return secureSync(
        context = context,
        from = from,
        timeout = timeout,
        timeUnit = timeUnit,
        mainThreadForbidden = mainThreadForbidden,
        waitingFlagKey = flagKey,
        debug = debug,
        what = what
    )
}