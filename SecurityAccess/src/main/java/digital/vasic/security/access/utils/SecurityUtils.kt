package digital.vasic.security.access.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "SecurityAccessKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH = 16

    /**
     * Generate a secure random salt for password hashing
     */
    fun generateSalt(length: Int = 32): ByteArray {
        val salt = ByteArray(length)
        SecureRandom.getInstanceStrong().nextBytes(salt)
        return salt
    }

    /**
     * Hash a password using PBKDF2 with HMAC-SHA256
     */
    fun hashPassword(password: String, salt: ByteArray, iterations: Int = 100000, keyLength: Int = 256): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLength)
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    /**
     * Verify a password against a stored hash
     */
    fun verifyPassword(password: String, salt: ByteArray, hash: ByteArray, iterations: Int = 100000, keyLength: Int = 256): Boolean {
        val testHash = hashPassword(password, salt, iterations, keyLength)
        return MessageDigest.isEqual(hash, testHash)
    }

    /**
     * Generate a secure PIN hash
     */
    fun hashPin(pin: String, salt: ByteArray): ByteArray {
        return hashPassword(pin, salt, 50000, 128) // Lower iterations for PIN for better UX
    }

    /**
     * Verify a PIN against a stored hash
     */
    fun verifyPin(pin: String, salt: ByteArray, hash: ByteArray): Boolean {
        return verifyPassword(pin, salt, hash, 50000, 128)
    }

    /**
     * Generate a cryptographically secure random PIN
     */
    fun generateSecurePin(length: Int = 4): String {
        val random = SecureRandom.getInstanceStrong()
        val pin = StringBuilder()

        repeat(length) {
            pin.append(random.nextInt(10))
        }

        return pin.toString()
    }

    /**
     * Encrypt data using AES-GCM with Android Keystore
     */
    fun encryptData(data: ByteArray): Pair<ByteArray, ByteArray> {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)

        return Pair(iv, encryptedData)
    }

    /**
     * Decrypt data using AES-GCM with Android Keystore
     */
    fun decryptData(iv: ByteArray, encryptedData: ByteArray): ByteArray {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)

        val gcmParameterSpec = GCMParameterSpec(TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec)

        return cipher.doFinal(encryptedData)
    }

    /**
     * Get or create the encryption key from Android Keystore
     */
    private fun getOrCreateKey(): SecretKey {
        return try {
            // Try to get existing key
            val keyStore = java.security.KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        } catch (e: Exception) {
            // Create new key if it doesn't exist
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    /**
     * Create encrypted shared preferences for sensitive data
     */
    fun createEncryptedSharedPreferences(context: android.content.Context, fileName: String): EncryptedSharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    /**
     * Generate a secure session ID
     */
    fun generateSessionId(): String {
        val random = SecureRandom.getInstanceStrong()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generate a device ID based on Android ID and other device characteristics
     */
    fun generateDeviceId(context: android.content.Context): String {
        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )

        val deviceInfo = "${android.os.Build.MANUFACTURER}_${android.os.Build.MODEL}_${android.os.Build.VERSION.SDK_INT}"
        val combined = "$androidId:$deviceInfo"

        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(combined.toByteArray())

        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Check if a password meets security requirements
     */
    fun validatePasswordStrength(
        password: String,
        minLength: Int = 8,
        requireUppercase: Boolean = false,
        requireLowercase: Boolean = false,
        requireNumbers: Boolean = false,
        requireSpecialChars: Boolean = false
    ): PasswordStrength {
        val issues = mutableListOf<String>()

        if (password.length < minLength) {
            issues.add("Password must be at least $minLength characters long")
        }

        if (requireUppercase && !password.any { it.isUpperCase() }) {
            issues.add("Password must contain at least one uppercase letter")
        }

        if (requireLowercase && !password.any { it.isLowerCase() }) {
            issues.add("Password must contain at least one lowercase letter")
        }

        if (requireNumbers && !password.any { it.isDigit() }) {
            issues.add("Password must contain at least one number")
        }

        if (requireSpecialChars && !password.any { !it.isLetterOrDigit() }) {
            issues.add("Password must contain at least one special character")
        }

        // Check for common weak patterns
        if (password.contains("123456") || password.contains("password") || password.contains("qwerty")) {
            issues.add("Password contains common weak patterns")
        }

        return PasswordStrength(
            isValid = issues.isEmpty(),
            score = calculatePasswordScore(password),
            issues = issues
        )
    }

    /**
     * Calculate password strength score (0-100)
     */
    private fun calculatePasswordScore(password: String): Int {
        var score = 0

        // Length bonus
        score += minOf(password.length * 4, 40)

        // Character variety bonus
        if (password.any { it.isUpperCase() }) score += 10
        if (password.any { it.isLowerCase() }) score += 10
        if (password.any { it.isDigit() }) score += 10
        if (password.any { !it.isLetterOrDigit() }) score += 15

        // Length bonus for longer passwords
        if (password.length >= 12) score += 10
        if (password.length >= 16) score += 10

        return minOf(score, 100)
    }

    data class PasswordStrength(
        val isValid: Boolean,
        val score: Int,
        val issues: List<String>
    )
}