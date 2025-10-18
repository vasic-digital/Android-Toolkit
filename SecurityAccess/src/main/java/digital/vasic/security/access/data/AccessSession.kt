package digital.vasic.security.access.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "access_sessions")
data class AccessSession(
    @PrimaryKey
    val sessionId: String,

    // Session details
    val accessMethod: AccessMethod,
    val biometricType: BiometricType? = null,
    val deviceId: String,
    val ipAddress: String? = null,

    // Session state
    val isActive: Boolean = true,
    val isLocked: Boolean = false,
    val lockReason: String? = null,

    // Timing information
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastActivityAt: LocalDateTime = LocalDateTime.now(),
    val expiresAt: LocalDateTime,
    val lockedAt: LocalDateTime? = null,

    // Usage statistics
    val accessCount: Int = 0,
    val failedAttempts: Int = 0,
    val totalDurationMs: Long = 0,

    // Security metadata
    val userAgent: String? = null,
    val location: String? = null,
    val isTrustedDevice: Boolean = false
)