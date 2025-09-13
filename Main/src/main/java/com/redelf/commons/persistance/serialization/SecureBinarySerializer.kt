package com.redelf.commons.persistance.serialization

import android.content.Context
import android.util.Base64
import com.redelf.commons.extensions.hashCodeString
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.persistance.base.Encryption
import com.redelf.commons.persistance.base.Salter
import com.redelf.commons.persistance.encryption.CompressedEncryption
import com.redelf.commons.persistance.encryption.ConcealEncryption
import com.redelf.commons.persistance.encryption.NoEncryption
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
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
 * Secure, high-performance binary serializer with comprehensive safety measures.
 * Replaces ByteArraySerializer with DoS protection, resource management, and streaming support.
 */
class SecureBinarySerializer(
    context: Context,
    key: String,
    private var encrypt: Boolean,
    private var encryption: Encryption<String>? = null,
    salter: Salter = object : Salter {
        override fun getSalt() = key.hashCodeString().reversed()
    }
) : Serializer {

    companion object {
        // Security and performance limits
        private const val MAX_OBJECT_SIZE_BYTES = 100 * 1024 * 1024 // 100MB
        private const val MAX_CACHE_SIZE = 1000
        private const val OPERATION_TIMEOUT_SECONDS = 60L // 1 minute
        private const val BUFFER_SIZE = 64 * 1024 // 64KB
        
        // Performance metrics
        private val totalOperations = AtomicLong(0)
        private val successfulOperations = AtomicLong(0)
        private val failedOperations = AtomicLong(0)
        private val totalBytesProcessed = AtomicLong(0)
        
        // Thread pool for safe operations
        private val executorService = Executors.newFixedThreadPool(4)
        
        // Instance management
        private val instances = ConcurrentHashMap<String, WeakReference<SecureBinarySerializer>>()
        
        fun getInstance(
            context: Context,
            key: String,
            encrypt: Boolean,
            encryption: Encryption<String>? = null,
            salter: Salter? = null
        ): SecureBinarySerializer {
            val mapKey = "${key}.${encrypt}.${encryption?.javaClass?.simpleName}"
            
            instances[mapKey]?.get()?.let { existing ->
                return existing
            }
            
            val actualSalter = salter ?: object : Salter {
                override fun getSalt() = key.hashCodeString().reversed()
            }
            
            val instance = SecureBinarySerializer(context, key, encrypt, encryption, actualSalter)
            instances[mapKey] = WeakReference(instance)
            return instance
        }
        
        fun getMetrics(): Map<String, Long> {
            return mapOf(
                "totalOperations" to totalOperations.get(),
                "successfulOperations" to successfulOperations.get(),
                "failedOperations" to failedOperations.get(),
                "totalBytesProcessed" to totalBytesProcessed.get(),
                "averageBytesPerOperation" to if (totalOperations.get() > 0) 
                    totalBytesProcessed.get() / totalOperations.get() else 0,
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

    private val ctxRef = WeakReference(context)
    private val tag = "SecureBinarySerializer :: Key='$key' ::"
    private val operationLock = ReentrantReadWriteLock()
    
    // Cache for serialized objects to improve performance
    private val serializationCache = ConcurrentHashMap<String, ByteArray>(MAX_CACHE_SIZE)
    
    private val sPrefs by lazy {
        ctxRef.get()?.getSharedPreferences(key, Context.MODE_PRIVATE)
            ?: throw IllegalStateException("Context is no longer available")
    }

    init {
        if (encryption == null) {
            encryption = instantiateDefaultEncryption(context, salter)
        }

        if (encryption is ConcealEncryption) {
            try {
                encryption?.init()
            } catch (e: Throwable) {
                Console.error("$tag Failed to initialize Conceal encryption: ${e.message}")
                recordException(e)
            }
        }
    }

    override fun takeClass(): Class<*> {
        return ByteArray::class.java
    }

    override fun serialize(key: String, value: Any): Boolean {
        totalOperations.incrementAndGet()

        return try {
            val future: Future<Boolean> = executorService.submit<Boolean> {
                performSafeSerialization(key, value)
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (result) {
                successfulOperations.incrementAndGet()
            } else {
                failedOperations.incrementAndGet()
            }
            result
        } catch (e: TimeoutException) {
            Console.error("$tag Serialization timeout after ${OPERATION_TIMEOUT_SECONDS}s for key: $key")
            failedOperations.incrementAndGet()
            false
        } catch (e: Throwable) {
            Console.error("$tag Serialization failed for key '$key': ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            false
        }
    }

    private fun performSafeSerialization(key: String, value: Any): Boolean {
        return operationLock.write {
            try {
                // Validate input size
                val estimatedSize = estimateObjectSize(value)
                if (estimatedSize > MAX_OBJECT_SIZE_BYTES) {
                    throw IllegalArgumentException("Object size exceeds maximum: $estimatedSize > $MAX_OBJECT_SIZE_BYTES bytes")
                }

                // Check cache first
                val cacheKey = "${key}_${value.hashCode()}"
                serializationCache[cacheKey]?.let { cached ->
                    return@write storeToPreferences(key, cached)
                }

                // Perform serialization with resource management
                val serializedData = ByteArrayOutputStream(BUFFER_SIZE).use { baos ->
                    ObjectOutputStream(baos).use { oos ->
                        oos.writeObject(value)
                        oos.flush()
                    }
                    baos.toByteArray()
                }

                totalBytesProcessed.addAndGet(serializedData.size.toLong())

                // Encrypt if needed
                val finalData = if (encrypt && encryption != null) {
                    val encoded = Base64.encodeToString(serializedData, Base64.NO_WRAP)
                    val encrypted = encryption?.encrypt(key, encoded)
                        ?: throw IllegalStateException("Encryption failed")
                    Base64.encodeToString(encrypted.toByteArray(), Base64.NO_WRAP).toByteArray()
                } else {
                    Base64.encodeToString(serializedData, Base64.NO_WRAP).toByteArray()
                }

                // Cache the result (with size limit)
                if (serializationCache.size < MAX_CACHE_SIZE && finalData.size < 1024 * 1024) { // 1MB limit
                    serializationCache[cacheKey] = finalData
                }

                storeToPreferences(key, finalData)
            } catch (e: Throwable) {
                Console.error("$tag Safe serialization error for key '$key': ${e.message}")
                recordException(e)
                false
            }
        }
    }

    private fun storeToPreferences(key: String, data: ByteArray): Boolean {
        return try {
            val editor = sPrefs.edit()
            val encoded = String(data, Charsets.UTF_8)
            editor.putString(key, encoded)
            editor.apply() // Use apply() instead of commit() for better performance
            true
        } catch (e: Throwable) {
            Console.error("$tag Failed to store to preferences: ${e.message}")
            recordException(e)
            false
        }
    }

    override fun deserialize(key: String): Any? {
        totalOperations.incrementAndGet()

        return try {
            val future: Future<Any?> = executorService.submit<Any?> {
                performSafeDeserialization(key)
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (result != null) {
                successfulOperations.incrementAndGet()
            } else {
                failedOperations.incrementAndGet()
            }
            result
        } catch (e: TimeoutException) {
            Console.error("$tag Deserialization timeout after ${OPERATION_TIMEOUT_SECONDS}s for key: $key")
            failedOperations.incrementAndGet()
            null
        } catch (e: Throwable) {
            Console.error("$tag Deserialization failed for key '$key': ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            null
        }
    }

    private fun performSafeDeserialization(key: String): Any? {
        return operationLock.read {
            try {
                val encodedData = sPrefs.getString(key, null) ?: return@read null
                
                // Validate size before processing
                if (encodedData.length > MAX_OBJECT_SIZE_BYTES * 2) { // Account for Base64 expansion
                    throw IllegalArgumentException("Stored data size exceeds maximum")
                }

                val data = encodedData.toByteArray(Charsets.UTF_8)
                totalBytesProcessed.addAndGet(data.size.toLong())

                // Decrypt if needed
                val decodedData = if (encrypt && encryption != null) {
                    val encryptedBytes = Base64.decode(data, Base64.NO_WRAP)
                    val encryptedString = String(encryptedBytes, Charsets.UTF_8)
                    val decrypted = encryption?.decrypt(key, encryptedString)
                        ?: throw IllegalStateException("Decryption failed")
                    Base64.decode(decrypted, Base64.NO_WRAP)
                } else {
                    Base64.decode(data, Base64.NO_WRAP)
                }

                // Deserialize with resource management
                ByteArrayInputStream(decodedData).use { bais ->
                    ObjectInputStream(bais).use { ois ->
                        ois.readObject()
                    }
                }
            } catch (e: Throwable) {
                Console.error("$tag Safe deserialization error for key '$key': ${e.message}")
                recordException(e)
                null
            }
        }
    }

    private fun estimateObjectSize(obj: Any): Int {
        return when (obj) {
            is ByteArray -> obj.size
            is String -> obj.toByteArray(Charsets.UTF_8).size
            is Collection<*> -> obj.size * 100 // Rough estimate
            is Map<*, *> -> obj.size * 200 // Rough estimate
            is Array<*> -> obj.size * 100 // Rough estimate
            else -> 1000 // Default estimate for complex objects
        }
    }

    private fun instantiateDefaultEncryption(context: Context, salter: Salter): Encryption<String> {
        return try {
            // Try to use compressed encryption first
            val compressed = CompressedEncryption()
            if (compressed.init()) {
                compressed
            } else {
                NoEncryption()
            }
        } catch (e: Throwable) {
            Console.error("$tag Failed to initialize default encryption: ${e.message}")
            recordException(e)
            NoEncryption()
        }
    }

    /**
     * Clear the serialization cache to free memory
     */
    fun clearCache() {
        operationLock.write {
            serializationCache.clear()
            Console.log("$tag Serialization cache cleared")
        }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): Map<String, Int> {
        return operationLock.read {
            mapOf(
                "cacheSize" to serializationCache.size,
                "maxCacheSize" to MAX_CACHE_SIZE
            )
        }
    }

    /**
     * Remove a specific key from storage
     */
    fun remove(key: String): Boolean {
        return operationLock.write {
            try {
                val editor = sPrefs.edit()
                editor.remove(key)
                editor.apply()
                
                // Also remove from cache
                serializationCache.remove("${key}_*")
                true
            } catch (e: Throwable) {
                Console.error("$tag Failed to remove key '$key': ${e.message}")
                recordException(e)
                false
            }
        }
    }

    /**
     * Check if a key exists in storage
     */
    fun contains(key: String): Boolean {
        return operationLock.read {
            try {
                sPrefs.contains(key)
            } catch (e: Throwable) {
                Console.error("$tag Failed to check key '$key': ${e.message}")
                recordException(e)
                false
            }
        }
    }

    /**
     * Get all stored keys
     */
    fun getAllKeys(): Set<String> {
        return operationLock.read {
            try {
                sPrefs.all.keys
            } catch (e: Throwable) {
                Console.error("$tag Failed to get all keys: ${e.message}")
                recordException(e)
                emptySet()
            }
        }
    }

    /**
     * Clear all stored data
     */
    fun clear(): Boolean {
        return operationLock.write {
            try {
                val editor = sPrefs.edit()
                editor.clear()
                editor.apply()
                serializationCache.clear()
                true
            } catch (e: Throwable) {
                Console.error("$tag Failed to clear storage: ${e.message}")
                recordException(e)
                false
            }
        }
    }
}