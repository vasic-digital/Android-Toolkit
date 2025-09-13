package com.redelf.commons.persistance

import com.redelf.commons.extensions.forClassName
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.base.Parser
import com.redelf.commons.persistance.base.Serializer
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
 * Secure, high-performance data serializer with comprehensive safety measures.
 * Replaces DataSerializer with DoS protection, resource management, and performance optimization.
 */
internal class SecureDataSerializer(private val parser: Obtain<Parser>) : Serializer {

    companion object {
        // Security and performance limits
        private const val MAX_COLLECTION_SIZE = 100000 // 100K items
        private const val MAX_MAP_SIZE = 50000 // 50K entries
        private const val MAX_JSON_SIZE_BYTES = 50 * 1024 * 1024 // 50MB
        private const val OPERATION_TIMEOUT_SECONDS = 60L // 1 minute
        private const val MAX_CACHE_SIZE = 1000
        
        // Performance metrics
        private val totalSerializations = AtomicLong(0)
        private val totalDeserializations = AtomicLong(0)
        private val successfulOperations = AtomicLong(0)
        private val failedOperations = AtomicLong(0)
        
        // Thread pool for safe operations
        private val executorService = Executors.newFixedThreadPool(4)
        
        fun getMetrics(): Map<String, Long> {
            return mapOf(
                "totalSerializations" to totalSerializations.get(),
                "totalDeserializations" to totalDeserializations.get(),
                "successfulOperations" to successfulOperations.get(),
                "failedOperations" to failedOperations.get(),
                "successRate" to if ((totalSerializations.get() + totalDeserializations.get()) > 0) 
                    (successfulOperations.get() * 100) / (totalSerializations.get() + totalDeserializations.get()) else 0
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

    private val tag = "SecureDataSerializer ::"
    private val operationLock = ReentrantReadWriteLock()
    
    // Cache for serialized DataInfo objects
    private val dataInfoCache = ConcurrentHashMap<String, DataInfo>(MAX_CACHE_SIZE)

    override fun <T> serialize(cipherText: String?, value: T): String? {
        if (cipherText.isNullOrEmpty() || value == null) {
            return null
        }

        totalSerializations.incrementAndGet()

        return try {
            val future: Future<String?> = executorService.submit<String?> {
                performSafeSerialization(cipherText, value)
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (result != null) {
                successfulOperations.incrementAndGet()
            } else {
                failedOperations.incrementAndGet()
            }
            result
        } catch (e: TimeoutException) {
            Console.error("$tag Serialization timeout after ${OPERATION_TIMEOUT_SECONDS}s for cipherText: $cipherText")
            failedOperations.incrementAndGet()
            null
        } catch (e: Throwable) {
            Console.error("$tag Serialization failed for cipherText '$cipherText': ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            null
        }
    }

    private fun <T> performSafeSerialization(cipherText: String, value: T): String? {
        return operationLock.write {
            try {
                // Validate collection sizes early
                validateObjectSize(value)

                var keyClassName: Class<*>? = null
                var valueClassName: Class<*>? = null
                val dataType: String

                when {
                    MutableList::class.java.isAssignableFrom(value!!.javaClass) -> {
                        val list = value as MutableList<*>
                        if (list.size > MAX_COLLECTION_SIZE) {
                            throw IllegalArgumentException("List size exceeds maximum: ${list.size} > $MAX_COLLECTION_SIZE")
                        }
                        
                        if (list.isNotEmpty()) {
                            keyClassName = list[0]?.javaClass
                        }
                        dataType = DataInfo.TYPE_LIST
                    }
                    
                    MutableMap::class.java.isAssignableFrom(value!!.javaClass) -> {
                        val map = value as MutableMap<*, *>
                        if (map.size > MAX_MAP_SIZE) {
                            throw IllegalArgumentException("Map size exceeds maximum: ${map.size} > $MAX_MAP_SIZE")
                        }
                        
                        dataType = DataInfo.TYPE_MAP
                        if (map.isNotEmpty()) {
                            val firstEntry = map.entries.first()
                            keyClassName = firstEntry.key?.javaClass
                            valueClassName = firstEntry.value?.javaClass
                        }
                    }
                    
                    MutableSet::class.java.isAssignableFrom(value!!.javaClass) -> {
                        val set = value as MutableSet<*>
                        if (set.size > MAX_COLLECTION_SIZE) {
                            throw IllegalArgumentException("Set size exceeds maximum: ${set.size} > $MAX_COLLECTION_SIZE")
                        }
                        
                        if (set.isNotEmpty()) {
                            keyClassName = set.first()?.javaClass
                        }
                        dataType = DataInfo.TYPE_SET
                    }
                    
                    else -> {
                        dataType = DataInfo.TYPE_OBJECT
                        keyClassName = value!!.javaClass
                    }
                }

                // Check cache first
                val cacheKey = "${cipherText}_${dataType}_${keyClassName?.name}_${valueClassName?.name}"
                dataInfoCache[cacheKey]?.let { cachedInfo ->
                    return@write serializeDataInfo(cachedInfo)
                }

                val dataInfo = DataInfo(
                    cipherText,
                    dataType,
                    keyClassName?.name,
                    valueClassName?.name,
                    keyClassName?.canonicalName?.forClassName(),
                    valueClassName?.canonicalName?.forClassName()
                )

                // Cache the DataInfo (with size limit)
                if (dataInfoCache.size < MAX_CACHE_SIZE) {
                    dataInfoCache[cacheKey] = dataInfo
                }

                serializeDataInfo(dataInfo)
            } catch (e: OutOfMemoryError) {
                Console.error("$tag Out of memory during serialization for cipherText: $cipherText")
                recordException(e)
                null
            } catch (e: Throwable) {
                Console.error("$tag Safe serialization error: ${e.message}")
                recordException(e)
                null
            }
        }
    }

    private fun serializeDataInfo(dataInfo: DataInfo): String? {
        return try {
            val result = parser.obtain().toJson(dataInfo)
            
            // Validate result size
            if (result != null && result.toByteArray(Charsets.UTF_8).size > MAX_JSON_SIZE_BYTES) {
                throw IllegalArgumentException("Serialized DataInfo exceeds maximum size: $MAX_JSON_SIZE_BYTES bytes")
            }
            
            result
        } catch (e: Throwable) {
            Console.error("$tag Failed to serialize DataInfo: ${e.message}")
            recordException(e)
            null
        }
    }

    override fun deserialize(plainText: String?): DataInfo? {
        if (isEmpty(plainText)) {
            return null
        }

        totalDeserializations.incrementAndGet()

        return try {
            // Validate input size
            val inputSize = plainText!!.toByteArray(Charsets.UTF_8).size
            if (inputSize > MAX_JSON_SIZE_BYTES) {
                throw IllegalArgumentException("Input JSON exceeds maximum size: $inputSize > $MAX_JSON_SIZE_BYTES bytes")
            }

            val future: Future<DataInfo?> = executorService.submit<DataInfo?> {
                performSafeDeserialization(plainText)
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (result != null) {
                successfulOperations.incrementAndGet()
            } else {
                failedOperations.incrementAndGet()
            }
            result
        } catch (e: TimeoutException) {
            Console.error("$tag Deserialization timeout after ${OPERATION_TIMEOUT_SECONDS}s")
            failedOperations.incrementAndGet()
            null
        } catch (e: Throwable) {
            Console.error("$tag Deserialization failed: ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            null
        }
    }

    private fun performSafeDeserialization(plainText: String): DataInfo? {
        return operationLock.read {
            try {
                // Check cache first
                val cacheKey = "deserialize_${plainText.hashCode()}"
                dataInfoCache[cacheKey]?.let { cached ->
                    return@read cached
                }

                val dataInfo = parser.obtain().fromJson<DataInfo?>(plainText, DataInfo::class.java)
                
                if (dataInfo != null) {
                    // Safely resolve class names
                    if (dataInfo.keyClazzName != null) {
                        try {
                            dataInfo.keyClazz = dataInfo.keyClazzName?.forClassName()
                        } catch (e: ClassNotFoundException) {
                            Console.error("$tag Key class not found: ${dataInfo.keyClazzName}")
                            // Continue without setting keyClazz - this is not fatal
                        }
                    }

                    if (dataInfo.valueClazzName != null) {
                        try {
                            dataInfo.valueClazz = dataInfo.valueClazzName?.forClassName()
                        } catch (e: ClassNotFoundException) {
                            Console.error("$tag Value class not found: ${dataInfo.valueClazzName}")
                            // Continue without setting valueClazz - this is not fatal
                        }
                    }

                    // Cache the result (with size limit)
                    if (dataInfoCache.size < MAX_CACHE_SIZE) {
                        dataInfoCache[cacheKey] = dataInfo
                    }
                }

                dataInfo
            } catch (e: Throwable) {
                Console.error("$tag Safe deserialization error: ${e.message}")
                Console.error("$tag Could not deserialize: ${plainText.take(100)}...")
                recordException(e)
                null
            }
        }
    }

    private fun validateObjectSize(obj: Any?) {
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
     * Clear the cache to free memory
     */
    fun clearCache() {
        operationLock.write {
            dataInfoCache.clear()
            Console.log("$tag DataInfo cache cleared")
        }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): Map<String, Int> {
        return operationLock.read {
            mapOf(
                "cacheSize" to dataInfoCache.size,
                "maxCacheSize" to MAX_CACHE_SIZE
            )
        }
    }

    /**
     * Get detailed serialization statistics
     */
    fun getDetailedStats(): Map<String, Any> {
        return operationLock.read {
            mapOf(
                "metrics" to getMetrics(),
                "cacheStats" to getCacheStats()
            )
        }
    }
}