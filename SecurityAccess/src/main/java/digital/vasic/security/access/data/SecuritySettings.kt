package digital.vasic.security.access.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "security_settings")
data class SecuritySettings(
    @PrimaryKey
    val id: String = "default",

    // Security configuration
    val isEnabled: Boolean = false,
    val accessMethod: AccessMethod = AccessMethod.NONE,
    val biometricType: BiometricType? = null,

    // PIN/Password settings
    val pinLength: Int = 4,
    val passwordMinLength: Int = 8,
    val requireSpecialChars: Boolean = false,
    val requireNumbers: Boolean = false,
    val requireUppercase: Boolean = false,

    // Session settings
    val sessionTimeoutMinutes: Long = 5,
    val maxFailedAttempts: Int = 5,
    val lockoutDurationMinutes: Long = 30,

    // Biometric settings
    val allowFingerprint: Boolean = true,
    val allowFaceRecognition: Boolean = true,
    val allowIris: Boolean = true,
    val requireStrongBiometric: Boolean = false,

    // UI settings
    val showBiometricPrompt: Boolean = true,
    val vibrateOnError: Boolean = true,
    val playSoundOnError: Boolean = true,

    // Timestamps
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val lastAccessAt: LocalDateTime? = null
)