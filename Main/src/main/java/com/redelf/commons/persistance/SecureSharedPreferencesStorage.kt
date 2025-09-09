package com.redelf.commons.persistance

import android.content.Context
import android.content.SharedPreferences
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.persistance.base.Storage
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Secure, high-performance SharedPreferences storage with comprehensive safety measures.
 * Replaces SharedPreferencesStorage with DoS protection, resource management, and validation.
 */
class SecureSharedPreferencesStorage private constructor(
    private val preferences: SharedPreferences
) : Storage<String?> {

    companion object {
        // Security and performance limits
        private const val MAX_KEY_LENGTH = 1000
        private const val MAX_VALUE_SIZE_BYTES = 10 * 1024 * 1024 // 10MB per value
        private const val MAX_TOTAL_ENTRIES = 10000
        private const val OPERATION_TIMEOUT_SECONDS = 30L
        
        // Performance metrics
        private val totalOperations = AtomicLong(0)
        private val successfulOperations = AtomicLong(0)
        private val failedOperations = AtomicLong(0)
        
        // Thread pool for safe operations
        private val executorService = Executors.newFixedThreadPool(4)
        
        // Instance management
        private val instances = ConcurrentHashMap<String, WeakReference<SecureSharedPreferencesStorage>>()
        
        fun getInstance(context: Context, name: String? = null): SecureSharedPreferencesStorage {
            val prefsName = name ?: context.packageName
            val instanceKey = "${context.packageName}_$prefsName"
            
            instances[instanceKey]?.get()?.let { existing ->
                return existing
            }
            
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val instance = SecureSharedPreferencesStorage(prefs)
            instances[instanceKey] = WeakReference(instance)
            return instance
        }
        
        fun getMetrics(): Map<String, Long> {
            return mapOf(
                "totalOperations" to totalOperations.get(),
                "successfulOperations" to successfulOperations.get(),
                "failedOperations" to failedOperations.get(),
                "successRate" to if (totalOperations.get() > 0) 
                    (successfulOperations.get() * 100) / totalOperations.get() else 0
            )
        }
        
        fun shutdown() {
            executorService.shutdown()
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow()
                }
            } catch (e: InterruptedException) {
                executorService.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
        
        private fun cleanupInstances() {
            val iterator = instances.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.get() == null) {
                    iterator.remove()
                }
            }
        }
    }

    constructor(context: Context, name: String? = null) : this(
        context.getSharedPreferences(
            name ?: context.packageName,
            Context.MODE_PRIVATE
        )
    )

    private val tag = "SecureSharedPreferencesStorage ::"
    private val operationLock = ReentrantReadWriteLock()
    
    // Cache for frequently accessed values
    private val readCache = ConcurrentHashMap<String, String?>(1000)
    
    override fun shutdown(): Boolean {
        return operationLock.write {
            try {
                readCache.clear()
                true
            } catch (e: Throwable) {
                Console.error("$tag Shutdown error: ${e.message}")
                recordException(e)
                false
            }
        }
    }

    override fun terminate(vararg args: Any): Boolean {
        return shutdown()
    }

    override fun initialize(ctx: Context) {
        // No initialization needed for SharedPreferences
        Console.log("$tag Initialized successfully")
    }

    override fun put(key: String?, value: String?): Boolean {
        if (key.isNullOrEmpty()) {
            Console.error("$tag Put operation failed: key is null or empty")
            return false
        }

        totalOperations.incrementAndGet()

        return try {
            val future: Future<Boolean> = executorService.submit<Boolean> {
                performSafePut(key, value)
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (result) {
                successfulOperations.incrementAndGet()
            } else {
                failedOperations.incrementAndGet()
            }
            result
        } catch (e: TimeoutException) {
            Console.error("$tag Put operation timeout after ${OPERATION_TIMEOUT_SECONDS}s for key: $key")
            failedOperations.incrementAndGet()
            false
        } catch (e: Throwable) {
            Console.error("$tag Put operation failed for key '$key': ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            false
        }
    }

    private fun performSafePut(key: String, value: String?): Boolean {
        return operationLock.write {
            try {
                // Validate key length
                if (key.length > MAX_KEY_LENGTH) {
                    throw IllegalArgumentException("Key length exceeds maximum: ${key.length} > $MAX_KEY_LENGTH")
                }

                // Validate value size
                val valueSize = value?.toByteArray(Charsets.UTF_8)?.size ?: 0
                if (valueSize > MAX_VALUE_SIZE_BYTES) {
                    throw IllegalArgumentException("Value size exceeds maximum: $valueSize > $MAX_VALUE_SIZE_BYTES bytes")
                }

                // Check total entries limit
                val currentSize = preferences.all.size
                if (currentSize >= MAX_TOTAL_ENTRIES && !preferences.contains(key)) {
                    throw IllegalArgumentException("Total entries exceed maximum: $currentSize >= $MAX_TOTAL_ENTRIES")
                }

                val editor = getEditor() ?: return@write false
                val result = editor.putString(key, value).commit()
                
                // Update cache on successful write
                if (result) {
                    if (readCache.size < 1000) { // Prevent cache overflow
                        readCache[key] = value
                    }
                } else {
                    Console.error("$tag SharedPreferences commit failed for key: $key")
                }
                
                result
            } catch (e: Throwable) {
                Console.error("$tag Safe put error for key '$key': ${e.message}")
                recordException(e)
                false
            }
        }
    }

    override fun get(key: String?, callback: OnObtain<String?>) {
        if (key.isNullOrEmpty()) {
            callback.onFailure(IllegalArgumentException("Key is null or empty"))
            return
        }

        totalOperations.incrementAndGet()

        try {
            val future: Future<String?> = executorService.submit<String?> {
                performSafeGet(key)
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            successfulOperations.incrementAndGet()
            callback.onCompleted(result)
        } catch (e: TimeoutException) {
            Console.error("$tag Get operation timeout after ${OPERATION_TIMEOUT_SECONDS}s for key: $key")
            failedOperations.incrementAndGet()
            callback.onFailure(e)
        } catch (e: Throwable) {
            Console.error("$tag Get operation failed for key '$key': ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            callback.onFailure(e)
        }
    }

    private fun performSafeGet(key: String): String? {
        return operationLock.read {
            try {
                // Check cache first
                readCache[key]?.let { cached ->
                    return@read cached
                }

                val result = preferences.getString(key, null)
                
                // Update cache on successful read (with size limit)
                if (readCache.size < 1000) {
                    readCache[key] = result
                }
                
                result
            } catch (e: Throwable) {
                Console.error("$tag Safe get error for key '$key': ${e.message}")
                recordException(e)
                null
            }
        }
    }

    override fun delete(key: String?): Boolean {
        if (key.isNullOrEmpty()) {
            Console.error("$tag Delete operation failed: key is null or empty")
            return false
        }

        totalOperations.incrementAndGet()

        return try {
            val future: Future<Boolean> = executorService.submit<Boolean> {
                performSafeDelete(key)
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (result) {
                successfulOperations.incrementAndGet()
            } else {
                failedOperations.incrementAndGet()
            }
            result
        } catch (e: TimeoutException) {
            Console.error("$tag Delete operation timeout after ${OPERATION_TIMEOUT_SECONDS}s for key: $key")
            failedOperations.incrementAndGet()
            false
        } catch (e: Throwable) {
            Console.error("$tag Delete operation failed for key '$key': ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            false
        }
    }

    private fun performSafeDelete(key: String): Boolean {
        return operationLock.write {
            try {
                val editor = getEditor() ?: return@write false
                val result = editor.remove(key).commit()
                
                // Remove from cache on successful delete
                if (result) {
                    readCache.remove(key)
                } else {
                    Console.error("$tag SharedPreferences remove commit failed for key: $key")
                }
                
                result
            } catch (e: Throwable) {
                Console.error("$tag Safe delete error for key '$key': ${e.message}")
                recordException(e)
                false
            }
        }
    }

    override fun deleteAll(): Boolean {
        totalOperations.incrementAndGet()

        return try {
            val future: Future<Boolean> = executorService.submit<Boolean> {
                performSafeDeleteAll()
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (result) {
                successfulOperations.incrementAndGet()
            } else {
                failedOperations.incrementAndGet()
            }
            result
        } catch (e: TimeoutException) {
            Console.error("$tag DeleteAll operation timeout after ${OPERATION_TIMEOUT_SECONDS}s")
            failedOperations.incrementAndGet()
            false
        } catch (e: Throwable) {
            Console.error("$tag DeleteAll operation failed: ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            false
        }
    }

    private fun performSafeDeleteAll(): Boolean {
        return operationLock.write {
            try {
                val editor = getEditor() ?: return@write false
                val result = editor.clear().commit()
                
                // Clear cache on successful clear
                if (result) {
                    readCache.clear()
                } else {
                    Console.error("$tag SharedPreferences clear commit failed")
                }
                
                result
            } catch (e: Throwable) {
                Console.error("$tag Safe deleteAll error: ${e.message}")
                recordException(e)
                false
            }
        }
    }

    override fun contains(key: String?, callback: OnObtain<Boolean?>) {
        if (key.isNullOrEmpty()) {
            callback.onFailure(IllegalArgumentException("Key is null or empty"))
            return
        }

        totalOperations.incrementAndGet()

        try {
            val future: Future<Boolean> = executorService.submit<Boolean> {
                performSafeContains(key)
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            successfulOperations.incrementAndGet()
            callback.onCompleted(result)
        } catch (e: TimeoutException) {
            Console.error("$tag Contains operation timeout after ${OPERATION_TIMEOUT_SECONDS}s for key: $key")
            failedOperations.incrementAndGet()
            callback.onFailure(e)
        } catch (e: Throwable) {
            Console.error("$tag Contains operation failed for key '$key': ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            callback.onFailure(e)
        }
    }

    private fun performSafeContains(key: String): Boolean {
        return operationLock.read {
            try {
                // Check cache first
                if (readCache.containsKey(key)) {
                    return@read true
                }
                
                preferences.contains(key)
            } catch (e: Throwable) {
                Console.error("$tag Safe contains error for key '$key': ${e.message}")
                recordException(e)
                false
            }
        }
    }

    override fun count(): Long {
        totalOperations.incrementAndGet()

        return try {
            val future: Future<Long> = executorService.submit<Long> {
                performSafeCount()
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            successfulOperations.incrementAndGet()
            result
        } catch (e: TimeoutException) {
            Console.error("$tag Count operation timeout after ${OPERATION_TIMEOUT_SECONDS}s")
            failedOperations.incrementAndGet()
            0L
        } catch (e: Throwable) {
            Console.error("$tag Count operation failed: ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            0L
        }
    }

    private fun performSafeCount(): Long {
        return operationLock.read {
            try {
                preferences.all.size.toLong()
            } catch (e: Throwable) {
                Console.error("$tag Safe count error: ${e.message}")
                recordException(e)
                0L
            }
        }
    }

    private fun getEditor(): SharedPreferences.Editor? {
        return try {
            preferences.edit()
        } catch (e: Throwable) {
            Console.error("$tag Failed to get SharedPreferences editor: ${e.message}")
            recordException(e)
            null
        }
    }

    /**
     * Clear the read cache to free memory
     */
    fun clearCache() {
        operationLock.write {
            readCache.clear()
            Console.log("$tag Read cache cleared")
        }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): Map<String, Int> {
        return operationLock.read {
            mapOf(
                "cacheSize" to readCache.size,
                "maxCacheSize" to 1000
            )
        }
    }

    /**
     * Get all stored keys (with size validation)
     */
    fun getAllKeys(): Set<String> {
        return operationLock.read {
            try {
                val allKeys = preferences.all.keys
                if (allKeys.size > MAX_TOTAL_ENTRIES) {
                    Console.error("$tag Warning: Key count exceeds maximum: ${allKeys.size} > $MAX_TOTAL_ENTRIES")
                }
                allKeys
            } catch (e: Throwable) {
                Console.error("$tag Failed to get all keys: ${e.message}")
                recordException(e)
                emptySet()
            }
        }
    }

    /**
     * Get storage size estimation in bytes
     */
    fun getStorageSizeEstimate(): Long {
        return operationLock.read {
            try {
                var totalSize = 0L
                preferences.all.forEach { (key, value) ->
                    totalSize += key.toByteArray(Charsets.UTF_8).size
                    when (value) {
                        is String -> totalSize += value.toByteArray(Charsets.UTF_8).size
                        else -> totalSize += 100 // Rough estimate for other types
                    }
                }
                totalSize
            } catch (e: Throwable) {
                Console.error("$tag Failed to estimate storage size: ${e.message}")
                recordException(e)
                0L
            }
        }
    }
}