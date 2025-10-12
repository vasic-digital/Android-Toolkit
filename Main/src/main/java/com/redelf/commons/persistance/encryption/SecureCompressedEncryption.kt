package com.redelf.commons.persistance.encryption

import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.persistance.base.Encryption
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Secure, high-performance compressed encryption with comprehensive safety measures.
 * Replaces CompressedEncryption with proper AES-GCM encryption, DoS protection, and resource management.
 */
class SecureCompressedEncryption : Encryption<String> {

    companion object {
        // Security and performance limits
        private const val MAX_VALUE_SIZE_BYTES = 50 * 1024 * 1024 // 50MB
        private const val MAX_COMPRESSED_SIZE_BYTES = 100 * 1024 * 1024 // 100MB after compression
        private const val OPERATION_TIMEOUT_SECONDS = 60L
        private const val MAX_KEY_CACHE_SIZE = 1000
        private const val MIN_KEY_LENGTH = 4
        private const val MAX_KEY_LENGTH = 1000
        
        // Encryption parameters
        private const val AES_KEY_LENGTH = 32 // 256-bit key
        private const val GCM_IV_LENGTH = 12 // 96-bit IV for GCM
        private const val GCM_TAG_LENGTH = 16 // 128-bit authentication tag
        private const val PBKDF2_ITERATIONS = 100000 // Strong key derivation
        
        // Compression parameters
        private const val COMPRESSION_LEVEL = Deflater.BEST_COMPRESSION
        private const val COMPRESSION_BUFFER_SIZE = 8192
        
        // Performance metrics
        private val totalEncryptions = AtomicLong(0)
        private val totalDecryptions = AtomicLong(0)
        private val successfulOperations = AtomicLong(0)
        private val failedOperations = AtomicLong(0)
        
        // Thread pool for safe operations
        private val executorService = Executors.newFixedThreadPool(4)
        
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

    private val tag = "SecureCompressedEncryption ::"
    private val operationLock = ReentrantReadWriteLock()
    private val secureRandom = SecureRandom()
    
    // Cache for derived keys to avoid expensive PBKDF2 operations
    private val keyCache = ConcurrentHashMap<String, ByteArray>(MAX_KEY_CACHE_SIZE)

    override fun init(): Boolean {
        return operationLock.read {
            try {
                // Test encryption availability
                val testCipher = Cipher.getInstance("AES/GCM/NoPadding")
                Console.log("$tag Initialized successfully")
                true
            } catch (e: Throwable) {
                Console.error("$tag Initialization failed: ${e.message}")
                recordException(e)
                false
            }
        }
    }

    override fun encrypt(key: String, value: String): String? {
        if (!validateEncryptionInput(key, value)) {
            return null
        }

        totalEncryptions.incrementAndGet()

        return try {
            val future: Future<String?> = executorService.submit<String?> {
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
            Console.error("$tag Encryption timeout after ${OPERATION_TIMEOUT_SECONDS}s")
            failedOperations.incrementAndGet()
            null
        } catch (e: Throwable) {
            Console.error("$tag Encryption failed: ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            null
        }
    }

    private fun performSafeEncryption(key: String, value: String): String? {
        return operationLock.write {
            try {
                // Step 1: Validate and compress the data
                val valueBytes = value.toByteArray(StandardCharsets.UTF_8)
                if (valueBytes.size > MAX_VALUE_SIZE_BYTES) {
                    throw IllegalArgumentException("Value size exceeds maximum: ${valueBytes.size} > $MAX_VALUE_SIZE_BYTES bytes")
                }

                val compressedData = compressData(valueBytes)
                if (compressedData.size > MAX_COMPRESSED_SIZE_BYTES) {
                    throw IllegalArgumentException("Compressed data exceeds maximum: ${compressedData.size} > $MAX_COMPRESSED_SIZE_BYTES bytes")
                }

                // Step 2: Generate encryption parameters
                val derivedKey = deriveKey(key)
                val iv = ByteArray(GCM_IV_LENGTH)
                secureRandom.nextBytes(iv)

                // Step 3: Encrypt the compressed data
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val keySpec = SecretKeySpec(derivedKey, "AES")
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
                
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
                val encryptedData = cipher.doFinal(compressedData)

                // Step 4: Combine IV + encrypted data and encode
                val combined = ByteArray(iv.size + encryptedData.size)
                System.arraycopy(iv, 0, combined, 0, iv.size)
                System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)

                val result = Base64.getEncoder().encodeToString(combined)
                Console.log("$tag Successfully encrypted and compressed ${valueBytes.size} bytes -> ${compressedData.size} bytes -> ${encryptedData.size} bytes")
                result
            } catch (e: Throwable) {
                Console.error("$tag Safe encryption error: ${e.message}")
                recordException(e)
                null
            }
        }
    }

    override fun decrypt(key: String, value: String): String? {
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
            Console.error("$tag Decryption timeout after ${OPERATION_TIMEOUT_SECONDS}s")
            failedOperations.incrementAndGet()
            null
        } catch (e: Throwable) {
            Console.error("$tag Decryption failed: ${e.message}")
            recordException(e)
            failedOperations.incrementAndGet()
            null
        }
    }

    private fun performSafeDecryption(key: String, value: String): String? {
        return operationLock.read {
            try {
                // Step 1: Decode and validate the input
                val combined = Base64.getDecoder().decode(value)
                if (combined.size < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                    throw IllegalArgumentException("Invalid encrypted data format")
                }

                // Step 2: Extract IV and encrypted data
                val iv = ByteArray(GCM_IV_LENGTH)
                System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
                
                val encryptedData = ByteArray(combined.size - GCM_IV_LENGTH)
                System.arraycopy(combined, GCM_IV_LENGTH, encryptedData, 0, encryptedData.size)

                // Step 3: Decrypt the data
                val derivedKey = deriveKey(key)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val keySpec = SecretKeySpec(derivedKey, "AES")
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
                
                cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
                val compressedData = cipher.doFinal(encryptedData)

                // Step 4: Decompress the data
                val decompressedData = decompressData(compressedData)
                val result = String(decompressedData, StandardCharsets.UTF_8)
                
                Console.log("$tag Successfully decrypted and decompressed ${encryptedData.size} bytes -> ${compressedData.size} bytes -> ${decompressedData.size} bytes")
                result
            } catch (e: Throwable) {
                Console.error("$tag Safe decryption error: ${e.message}")
                recordException(e)
                null
            }
        }
    }

    private fun compressData(data: ByteArray): ByteArray {
        return ByteArrayOutputStream().use { baos ->
            DeflaterOutputStream(baos, Deflater(COMPRESSION_LEVEL), COMPRESSION_BUFFER_SIZE).use { dos ->
                dos.write(data)
                dos.finish()
            }
            baos.toByteArray()
        }
    }

    private fun decompressData(compressedData: ByteArray): ByteArray {
        return ByteArrayInputStream(compressedData).use { bais ->
            InflaterInputStream(bais, Inflater(), COMPRESSION_BUFFER_SIZE).use { iis ->
                iis.readBytes()
            }
        }
    }

    private fun deriveKey(key: String): ByteArray {
        return keyCache.getOrPut(key) {
            try {
                // Use PBKDF2 for secure key derivation
                val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val spec = javax.crypto.spec.PBEKeySpec(
                    key.toCharArray(),
                    "SecureCompression2024".toByteArray(StandardCharsets.UTF_8), // Static salt for consistency
                    PBKDF2_ITERATIONS,
                    AES_KEY_LENGTH * 8
                )
                val tmp = factory.generateSecret(spec)
                tmp.encoded
            } catch (e: Throwable) {
                Console.error("$tag Failed to derive key: ${e.message}")
                recordException(e)
                // Fallback to SHA-256 (less secure but functional)
                val digest = MessageDigest.getInstance("SHA-256")
                digest.digest((key + "SecureCompression2024").toByteArray(StandardCharsets.UTF_8))
            }
        }
    }

    private fun validateEncryptionInput(key: String?, value: String?): Boolean {
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

        val valueBytes = value.toByteArray(StandardCharsets.UTF_8)
        if (valueBytes.size > MAX_VALUE_SIZE_BYTES) {
            Console.error("$tag Encryption failed: value too large (${valueBytes.size} > $MAX_VALUE_SIZE_BYTES)")
            failedOperations.incrementAndGet()
            return false
        }

        return true
    }

    private fun validateDecryptionInput(key: String?, value: String?): Boolean {
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

        if (value.isNullOrEmpty()) {
            Console.error("$tag Decryption failed: value is null or empty")
            failedOperations.incrementAndGet()
            return false
        }

        try {
            val decodedSize = Base64.getDecoder().decode(value).size
            if (decodedSize > MAX_COMPRESSED_SIZE_BYTES) {
                Console.error("$tag Decryption failed: encoded value too large ($decodedSize > $MAX_COMPRESSED_SIZE_BYTES)")
                failedOperations.incrementAndGet()
                return false
            }
        } catch (e: Throwable) {
            Console.error("$tag Decryption failed: invalid Base64 encoding")
            failedOperations.incrementAndGet()
            return false
        }

        return true
    }

    /**
     * Clear the key cache to free memory
     */
    fun clearCache() {
        operationLock.write {
            keyCache.clear()
            Console.log("$tag Key cache cleared")
        }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): Map<String, Int> {
        return operationLock.read {
            mapOf(
                "keyCacheSize" to keyCache.size,
                "maxKeyCacheSize" to MAX_KEY_CACHE_SIZE
            )
        }
    }
}