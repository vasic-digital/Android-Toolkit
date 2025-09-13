package com.redelf.commons.persistance

import com.redelf.commons.extensions.recordException
import com.redelf.commons.extensions.toClass
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.base.Converter
import com.redelf.commons.persistance.base.Parser
import java.lang.reflect.Type
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
 * Secure, high-performance data converter with comprehensive safety measures.
 * Replaces the unsafe DataConverter with DoS protection and resource management.
 */
@Suppress("UNCHECKED_CAST")
internal class SecureDataConverter(private val parser: Obtain<Parser>) : Converter {

    companion object {
        // Security and performance limits
        private const val MAX_JSON_SIZE_BYTES = 50 * 1024 * 1024 // 50MB
        private const val MAX_COLLECTION_SIZE = 100000 // 100K items
        private const val MAX_MAP_SIZE = 50000 // 50K entries
        private const val MAX_NESTING_DEPTH = 1000
        private const val OPERATION_TIMEOUT_SECONDS = 60L // 1 minute
        
        // Performance metrics
        private val totalOperations = AtomicLong(0)
        private val successfulOperations = AtomicLong(0)
        private val failedOperations = AtomicLong(0)
        
        // Thread pool for safe operations
        private val executorService = Executors.newFixedThreadPool(4)
        
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
    }

    private val tag = "SecureDataConverter ::"
    private val operationLock = ReentrantReadWriteLock()
    
    // Cache for parsed objects to improve performance
    private val parseCache = ConcurrentHashMap<String, Any>(1000)

    private fun debug() = Converter.Companion.DEBUG

    override fun <T> toString(value: T): String? {
        if (value == null) return null
        if (value is String) return value

        totalOperations.incrementAndGet()

        return try {
            val future: Future<String?> = executorService.submit<String?> {
                performSafeToString(value)
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            successfulOperations.incrementAndGet()
            result
        } catch (e: TimeoutException) {
            Console.error("$tag toString operation timed out after ${OPERATION_TIMEOUT_SECONDS}s")
            failedOperations.incrementAndGet()
            null
        } catch (e: Throwable) {
            Console.error("$tag toString failed: ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            null
        }
    }

    private fun <T> performSafeToString(value: T): String? {
        return operationLock.read {
            try {
                validateObjectSize(value)
                
                if (debug().get()) {
                    Console.log("$tag START :: Class = ${value?.javaClass?.canonicalName}")
                }

                val p = parser.obtain()
                val result = p.toJson(value)
                
                // Validate result size
                if (result != null && result.toByteArray(Charsets.UTF_8).size > MAX_JSON_SIZE_BYTES) {
                    throw IllegalArgumentException("Serialized JSON exceeds maximum size: $MAX_JSON_SIZE_BYTES bytes")
                }
                
                result
            } catch (e: Throwable) {
                Console.error("$tag Safe toString error: ${e.message}")
                recordException(e)
                null
            }
        }
    }

    override fun <T> fromString(value: String?, info: DataInfo?): T? {
        if (value == null || info == null) return null

        totalOperations.incrementAndGet()

        return try {
            validateInputSize(value)
            
            val future: Future<T?> = executorService.submit<T?> {
                performSafeFromString(value, info)
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            successfulOperations.incrementAndGet()
            result
        } catch (e: TimeoutException) {
            Console.error("$tag fromString operation timed out after ${OPERATION_TIMEOUT_SECONDS}s")
            failedOperations.incrementAndGet()
            null
        } catch (e: Throwable) {
            Console.error("$tag fromString failed: ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            null
        }
    }

    private fun <T> performSafeFromString(value: String, info: DataInfo): T? {
        return operationLock.read {
            try {
                val keyType = info.keyClazz
                val valueType = info.valueClazz

                when (info.dataType) {
                    DataInfo.TYPE_OBJECT -> toObjectSafe<T>(value, keyType?.toClass())
                    DataInfo.TYPE_LIST -> toListSafe<T>(value, keyType?.toClass())
                    DataInfo.TYPE_MAP -> toMapSafe<Any, Any, T>(value, keyType?.toClass(), valueType?.toClass())
                    DataInfo.TYPE_SET -> toSetSafe<T>(value, keyType?.toClass())
                    else -> null
                }
            } catch (e: Throwable) {
                Console.error("$tag Safe fromString error: ${e.message}")
                recordException(e)
                null
            }
        }
    }

    override fun <T> fromString(value: String?, type: Type?): T? {
        if (value == null || type == null) return null

        totalOperations.incrementAndGet()

        return try {
            validateInputSize(value)
            
            val future: Future<T?> = executorService.submit<T?> {
                performSafeFromStringType(value, type)
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            successfulOperations.incrementAndGet()
            result
        } catch (e: TimeoutException) {
            Console.error("$tag fromString(type) operation timed out after ${OPERATION_TIMEOUT_SECONDS}s")
            failedOperations.incrementAndGet()
            null
        } catch (e: Throwable) {
            Console.error("$tag fromString(type) failed: ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            null
        }
    }

    private fun <T> performSafeFromStringType(value: String, type: Type): T? {
        return operationLock.read {
            try {
                val cacheKey = "${type.typeName}:${value.hashCode()}"
                
                // Check cache first for performance
                parseCache[cacheKey]?.let { cached ->
                    if (debug().get()) {
                        Console.log("$tag Cache hit for type: ${type.typeName}")
                    }
                    return@read cached as T?
                }

                val p = parser.obtain()
                val result = p.fromJson<T>(value, type)
                
                // Cache successful parse (with size limit)
                if (result != null && parseCache.size < 1000) {
                    parseCache[cacheKey] = result as Any
                }
                
                result
            } catch (e: Throwable) {
                Console.error("$tag Safe fromString(type) error: ${e.message}")
                Console.error("$tag Tried to deserialize into '${type.typeName}' from '${value.take(100)}...'")
                recordException(e)
                null
            }
        }
    }

    override fun <T> fromString(value: String?, clazz: Class<T>?): T? {
        if (value == null || clazz == null) return null

        totalOperations.incrementAndGet()

        return try {
            validateInputSize(value)
            
            val future: Future<T?> = executorService.submit<T?> {
                performSafeFromStringClass(value, clazz)
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            successfulOperations.incrementAndGet()
            result
        } catch (e: TimeoutException) {
            Console.error("$tag fromString(class) operation timed out after ${OPERATION_TIMEOUT_SECONDS}s")
            failedOperations.incrementAndGet()
            null
        } catch (e: Throwable) {
            Console.error("$tag fromString(class) failed: ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            null
        }
    }

    private fun <T> performSafeFromStringClass(value: String, clazz: Class<T>): T? {
        return operationLock.read {
            try {
                val cacheKey = "${clazz.simpleName}:${value.hashCode()}"
                
                // Check cache first for performance
                parseCache[cacheKey]?.let { cached ->
                    if (debug().get()) {
                        Console.log("$tag Cache hit for class: ${clazz.simpleName}")
                    }
                    return@read cached as T?
                }

                val p = parser.obtain()
                val result = p.fromJson<T>(value, clazz)
                
                // Cache successful parse (with size limit)
                if (result != null && parseCache.size < 1000) {
                    parseCache[cacheKey] = result as Any
                }
                
                result
            } catch (e: Throwable) {
                Console.error("$tag Safe fromString(class) error: ${e.message}")
                Console.error("$tag Tried to deserialize into '${clazz.simpleName}' from '${value.take(100)}...'")
                recordException(e)
                null
            }
        }
    }

    @Throws(Exception::class)
    private fun <T> toObjectSafe(json: String, type: Class<*>?): T? {
        val p = parser.obtain()
        return p.fromJson<T>(json, type)
    }

    @Throws(Exception::class)
    private fun <T> toListSafe(json: String, type: Class<*>?): T {
        if (type == null) {
            return ArrayList<Any>() as T
        }

        val p = parser.obtain()
        
        // Use direct parsing instead of double serialization for performance
        val result = p.fromJson<List<Any>>(json, List::class.java) ?: return ArrayList<Any>() as T
        
        // Validate collection size
        if (result.size > MAX_COLLECTION_SIZE) {
            throw IllegalArgumentException("List size exceeds maximum allowed: ${result.size} > $MAX_COLLECTION_SIZE")
        }
        
        return result as T
    }

    @Throws(Exception::class)
    private fun <T> toSetSafe(json: String, type: Class<*>?): T? {
        if (type == null) {
            return HashSet<Any>() as T
        }

        val p = parser.obtain()
        val result = p.fromJson<Set<Any>>(json, Set::class.java) ?: return HashSet<Any>() as T
        
        // Validate collection size
        if (result.size > MAX_COLLECTION_SIZE) {
            throw IllegalArgumentException("Set size exceeds maximum allowed: ${result.size} > $MAX_COLLECTION_SIZE")
        }
        
        return result as T
    }

    @Throws(Exception::class)
    private fun <K, V, T> toMapSafe(json: String, keyType: Class<*>?, valueType: Class<*>?): T? {
        if (keyType == null || valueType == null) {
            return HashMap<Any, Any>() as T
        }

        val p = parser.obtain()
        val result = p.fromJson<Map<Any, Any>>(json, Map::class.java) ?: return HashMap<Any, Any>() as T
        
        // Validate map size
        if (result.size > MAX_MAP_SIZE) {
            throw IllegalArgumentException("Map size exceeds maximum allowed: ${result.size} > $MAX_MAP_SIZE")
        }
        
        return result as T
    }

    private fun validateInputSize(input: String) {
        val sizeBytes = input.toByteArray(Charsets.UTF_8).size
        if (sizeBytes > MAX_JSON_SIZE_BYTES) {
            throw IllegalArgumentException("Input JSON exceeds maximum size: $sizeBytes > $MAX_JSON_SIZE_BYTES bytes")
        }
    }

    private fun validateObjectSize(obj: Any?) {
        // Basic size estimation - can be enhanced with deep object analysis
        when (obj) {
            is Collection<*> -> {
                if (obj.size > MAX_COLLECTION_SIZE) {
                    throw IllegalArgumentException("Collection size exceeds maximum: ${obj.size} > $MAX_COLLECTION_SIZE")
                }
            }
            is Map<*, *> -> {
                if (obj.size > MAX_MAP_SIZE) {
                    throw IllegalArgumentException("Map size exceeds maximum: ${obj.size} > $MAX_MAP_SIZE")
                }
            }
            is Array<*> -> {
                if (obj.size > MAX_COLLECTION_SIZE) {
                    throw IllegalArgumentException("Array size exceeds maximum: ${obj.size} > $MAX_COLLECTION_SIZE")
                }
            }
        }
    }

    /**
     * Clear the parse cache to free memory
     */
    fun clearCache() {
        operationLock.write {
            parseCache.clear()
            if (debug().get()) {
                Console.log("$tag Parse cache cleared")
            }
        }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): Map<String, Int> {
        return operationLock.read {
            mapOf(
                "cacheSize" to parseCache.size,
                "maxCacheSize" to 1000
            )
        }
    }
}