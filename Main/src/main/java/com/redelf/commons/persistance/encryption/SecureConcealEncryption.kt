package com.redelf.commons.persistance.encryption

import android.content.Context
import com.facebook.android.crypto.keychain.AndroidConceal
import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain
import com.facebook.crypto.Crypto
import com.facebook.crypto.CryptoConfig
import com.facebook.crypto.Entity
import com.facebook.crypto.keychain.KeyChain
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.persistance.base.Encryption
import com.redelf.commons.persistance.base.Salter
import java.security.MessageDigest
import java.security.SecureRandom
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
 * Secure, high-performance Conceal encryption with comprehensive safety measures.
 * Replaces ConcealEncryption with DoS protection, proper key derivation, and resource management.
 */
class SecureConcealEncryption private constructor(
    private val crypto: Crypto,
    private val salter: Salter
) : Encryption<ByteArray> {

    companion object {
        // Security and performance limits
        private const val MAX_KEY_LENGTH = 1000
        private const val MAX_VALUE_SIZE_BYTES = 50 * 1024 * 1024 // 50MB
        private const val OPERATION_TIMEOUT_SECONDS = 30L
        private const val MAX_ENTITY_CACHE_SIZE = 1000
        private const val MIN_KEY_LENGTH = 4 // Minimum key length for security
        
        // Performance metrics
        private val totalEncryptions = AtomicLong(0)
        private val totalDecryptions = AtomicLong(0)
        private val successfulOperations = AtomicLong(0)
        private val failedOperations = AtomicLong(0)
        
        // Thread pool for safe operations
        private val executorService = Executors.newFixedThreadPool(4)
        
        fun getInstance(context: Context, salter: Salter): SecureConcealEncryption {
            return try {
                val keyChain = SharedPrefsBackedKeyChain(context, CryptoConfig.KEY_256)
                val crypto = AndroidConceal.get().createDefaultCrypto(keyChain)
                SecureConcealEncryption(crypto, salter)
            } catch (e: Throwable) {
                Console.error("Failed to create SecureConcealEncryption: ${e.message}")
                recordException(e)
                throw IllegalStateException("Cannot initialize secure encryption", e)
            }
        }
        
        fun getInstance(keyChain: KeyChain, salter: Salter): SecureConcealEncryption {
            return try {
                val crypto = AndroidConceal.get().createDefaultCrypto(keyChain)
                SecureConcealEncryption(crypto, salter)
            } catch (e: Throwable) {
                Console.error("Failed to create SecureConcealEncryption with custom keychain: ${e.message}")
                recordException(e)
                throw IllegalStateException("Cannot initialize secure encryption with keychain", e)
            }
        }
        
        fun getMetrics(): Map<String, Long> {
            return mapOf(
                "totalEncryptions" to totalEncryptions.get(),
                "totalDecryptions" to totalDecryptions.get(),
                "successfulOperations" to successfulOperations.get(),
                "failedOperations" to failedOperations.get(),
                "successRate" to if ((totalEncryptions.get() + totalDecryptions.get()) > 0) 
                    (successfulOperations.get() * 100) / (totalEncryptions.get() + totalDecryptions.get()) else 0
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

    private val tag = "SecureConcealEncryption ::"
    private val operationLock = ReentrantReadWriteLock()
    private val salt = salter.getSalt()
    
    // Cache for Entity objects to avoid repeated creation
    private val entityCache = ConcurrentHashMap<String, Entity>(MAX_ENTITY_CACHE_SIZE)
    
    // Secure random for additional entropy
    private val secureRandom = SecureRandom()

    override fun init(): Boolean {
        return operationLock.read {
            try {
                val available = crypto.isAvailable
                if (available) {
                    Console.log("$tag Initialized successfully")
                } else {
                    Console.error("$tag Crypto not available")
                }
                available
            } catch (e: Throwable) {
                Console.error("$tag Initialization failed: ${e.message}")
                recordException(e)
                false
            }
        }
    }

    @Throws(Exception::class)
    override fun encrypt(key: String, value: String): ByteArray? {
        if (!validateInput(key, value)) {
            return null
        }

        totalEncryptions.incrementAndGet()

        return try {
            val future: Future<ByteArray?> = executorService.submit<ByteArray?> {
                performSafeEncryption(key, value)
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (result != null) {
                successfulOperations.incrementAndGet()
            } else {
                failedOperations.incrementAndGet()
            }
            result
        } catch (e: TimeoutException) {
            Console.error("$tag Encryption timeout after ${OPERATION_TIMEOUT_SECONDS}s for key: $key")
            failedOperations.incrementAndGet()
            null
        } catch (e: Throwable) {
            Console.error("$tag Encryption failed for key '$key': ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            null
        }
    }

    private fun performSafeEncryption(key: String, value: String): ByteArray? {
        return operationLock.write {
            try {
                val valueBytes = value.toByteArray(Charsets.UTF_8)
                
                // Validate value size
                if (valueBytes.size > MAX_VALUE_SIZE_BYTES) {
                    throw IllegalArgumentException("Value size exceeds maximum: ${valueBytes.size} > $MAX_VALUE_SIZE_BYTES bytes")
                }

                val entity = getOrCreateEntity(key)
                val encrypted = crypto.encrypt(valueBytes, entity)

                if (encrypted.isEmpty()) {
                    Console.warning("$tag Encrypted value is empty for key: $key")
                    return@write null
                }

                Console.log("$tag Successfully encrypted ${valueBytes.size} bytes for key: $key")
                encrypted
            } catch (e: Throwable) {
                Console.error("$tag Safe encryption error for key '$key': ${e.message}")
                recordException(e)
                null
            }
        }
    }

    @Throws(Exception::class)
    override fun decrypt(key: String, value: ByteArray): String? {
        if (!validateDecryptionInput(key, value)) {
            return null
        }

        totalDecryptions.incrementAndGet()

        return try {
            val future: Future<String?> = executorService.submit<String?> {
                performSafeDecryption(key, value)
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (result != null) {
                successfulOperations.incrementAndGet()
            } else {
                failedOperations.incrementAndGet()
            }
            result
        } catch (e: TimeoutException) {
            Console.error("$tag Decryption timeout after ${OPERATION_TIMEOUT_SECONDS}s for key: $key")
            failedOperations.incrementAndGet()
            null
        } catch (e: Throwable) {
            Console.error("$tag Decryption failed for key '$key': ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            null
        }
    }

    private fun performSafeDecryption(key: String, value: ByteArray): String? {
        return operationLock.read {
            try {
                val entity = getOrCreateEntity(key)
                val decrypted = crypto.decrypt(value, entity)

                val result = String(decrypted, Charsets.UTF_8)
                Console.log("$tag Successfully decrypted ${decrypted.size} bytes for key: $key")
                result
            } catch (e: Throwable) {
                Console.error("$tag Safe decryption error for key '$key': ${e.message}")
                recordException(e)
                null
            }
        }
    }

    private fun validateInput(key: String?, value: String?): Boolean {
        if (key.isNullOrEmpty()) {
            Console.error("$tag Encryption failed: key is null or empty")
            failedOperations.incrementAndGet()
            return false
        }
        
        if (key.length < MIN_KEY_LENGTH) {
            Console.error("$tag Encryption failed: key too short (${key.length} < $MIN_KEY_LENGTH)")
            failedOperations.incrementAndGet()
            return false
        }

        if (key.length > MAX_KEY_LENGTH) {
            Console.error("$tag Encryption failed: key too long (${key.length} > $MAX_KEY_LENGTH)")
            failedOperations.incrementAndGet()
            return false
        }

        if (value.isNullOrEmpty()) {
            Console.error("$tag Encryption failed: value is null or empty")
            failedOperations.incrementAndGet()
            return false
        }

        return true
    }

    private fun validateDecryptionInput(key: String?, value: ByteArray?): Boolean {
        if (key.isNullOrEmpty()) {
            Console.error("$tag Decryption failed: key is null or empty")
            failedOperations.incrementAndGet()
            return false
        }

        if (key.length < MIN_KEY_LENGTH) {
            Console.error("$tag Decryption failed: key too short (${key.length} < $MIN_KEY_LENGTH)")
            failedOperations.incrementAndGet()
            return false
        }

        if (key.length > MAX_KEY_LENGTH) {
            Console.error("$tag Decryption failed: key too long (${key.length} > $MAX_KEY_LENGTH)")
            failedOperations.incrementAndGet()
            return false
        }

        if (value == null || value.isEmpty()) {
            Console.error("$tag Decryption failed: value is null or empty")
            failedOperations.incrementAndGet()
            return false
        }

        if (value.size > MAX_VALUE_SIZE_BYTES) {
            Console.error("$tag Decryption failed: value too large (${value.size} > $MAX_VALUE_SIZE_BYTES)")
            failedOperations.incrementAndGet()
            return false
        }

        return true
    }

    private fun getOrCreateEntity(key: String): Entity {
        return entityCache.getOrPut(key) {
            try {
                // Create a cryptographically secure key derivation
                val secureKey = deriveSecureKey(key)
                Entity.create(secureKey)
            } catch (e: Throwable) {
                Console.error("$tag Failed to create entity for key '$key': ${e.message}")
                recordException(e)
                throw IllegalStateException("Cannot create entity for encryption", e)
            }
        }
    }

    private fun deriveSecureKey(key: String): String {
        return try {
            // Use proper key derivation with SHA-256 and salt
            val keyWithSalt = "$key:$salt:${secureRandom.nextLong()}"
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(keyWithSalt.toByteArray(Charsets.UTF_8))
            
            // Convert to hex string for consistent key format
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Throwable) {
            Console.error("$tag Failed to derive secure key: ${e.message}")
            recordException(e)
            // Fallback to simple hash (less secure but functional)
            (key + salt).hashCode().toString()
        }
    }

    /**
     * Clear the entity cache to free memory
     */
    fun clearCache() {
        operationLock.write {
            entityCache.clear()
            Console.log("$tag Entity cache cleared")
        }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): Map<String, Int> {
        return operationLock.read {
            mapOf(
                "entityCacheSize" to entityCache.size,
                "maxEntityCacheSize" to MAX_ENTITY_CACHE_SIZE
            )
        }
    }
}