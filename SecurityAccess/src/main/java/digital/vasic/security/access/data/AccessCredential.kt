package digital.vasic.security.access.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "access_credentials")
data class AccessCredential(
    @PrimaryKey
    val id: String = "default",

    // Credential data (encrypted)
    val hashedPin: String? = null,
    val hashedPassword: String? = null,
    val salt: String? = null,

    // Biometric data
    val biometricTemplate: ByteArray? = null,
    val biometricType: BiometricType? = null,

    // Security metadata
    val encryptionAlgorithm: String = "PBKDF2WithHmacSHA256",
    val keyLength: Int = 256,
    val iterations: Int = 100000,

    // Status
    val isActive: Boolean = true,
    val isCompromised: Boolean = false,

    // Timestamps
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val lastUsedAt: LocalDateTime? = null,
    val expiresAt: LocalDateTime? = null
)