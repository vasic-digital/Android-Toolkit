package digital.vasic.security.access.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "access_attempts")
data class AccessAttempt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Attempt details
    val accessMethod: AccessMethod,
    val biometricType: BiometricType? = null,
    val status: AccessStatus,
    val errorCode: Int? = null,
    val errorMessage: String? = null,

    // Context information
    val deviceId: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,

    // Location data (if available)
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracy: Float? = null,

    // Timing information
    val attemptDurationMs: Long,
    val timestamp: LocalDateTime = LocalDateTime.now(),

    // Session information
    val sessionId: String? = null,
    val isSuspicious: Boolean = false
)