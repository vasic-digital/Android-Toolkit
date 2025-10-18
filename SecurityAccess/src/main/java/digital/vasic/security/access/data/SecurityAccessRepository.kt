package digital.vasic.security.access.data

import android.content.Context
import digital.vasic.security.access.database.SecurityAccessDatabase
import digital.vasic.security.access.utils.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class SecurityAccessRepository private constructor(
    private val database: SecurityAccessDatabase
) {

    // Settings operations
    fun getSecuritySettings(): Flow<SecuritySettings?> {
        return database.securitySettingsDao().getSettings()
    }

    suspend fun getSecuritySettingsSync(): SecuritySettings? {
        return database.securitySettingsDao().getSettingsSync()
    }

    suspend fun saveSecuritySettings(settings: SecuritySettings) {
        database.securitySettingsDao().insertSettings(settings)
    }

    suspend fun updateSecuritySettingsEnabled(enabled: Boolean) {
        database.securitySettingsDao().updateEnabledStatus(
            enabled = enabled,
            updatedAt = LocalDateTime.now()
        )
    }

    // Credential operations
    fun getAccessCredential(): Flow<AccessCredential?> {
        return database.accessCredentialDao().getCredential()
    }

    suspend fun getAccessCredentialSync(): AccessCredential? {
        return database.accessCredentialDao().getCredentialSync()
    }

    suspend fun saveAccessCredential(credential: AccessCredential) {
        database.accessCredentialDao().insertCredential(credential)
    }

    suspend fun updatePin(pin: String) {
        val salt = SecurityUtils.generateSalt()
        val hashedPin = SecurityUtils.hashPin(pin, salt)

        database.accessCredentialDao().updatePin(
            hashedPin = hashedPin.joinToString("") { "%02x".format(it) },
            salt = salt.joinToString("") { "%02x".format(it) },
            updatedAt = LocalDateTime.now()
        )
    }

    suspend fun updatePassword(password: String) {
        val salt = SecurityUtils.generateSalt()
        val hashedPassword = SecurityUtils.hashPassword(password, salt)

        database.accessCredentialDao().updatePassword(
            hashedPassword = hashedPassword.joinToString("") { "%02x".format(it) },
            salt = salt.joinToString("") { "%02x".format(it) },
            updatedAt = LocalDateTime.now()
        )
    }

    suspend fun verifyPin(pin: String): Boolean {
        val credential = getAccessCredentialSync() ?: return false
        val salt = credential.salt?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray() ?: return false
        val hash = credential.hashedPin?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray() ?: return false

        return SecurityUtils.verifyPin(pin, salt, hash)
    }

    suspend fun verifyPassword(password: String): Boolean {
        val credential = getAccessCredentialSync() ?: return false
        val salt = credential.salt?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray() ?: return false
        val hash = credential.hashedPassword?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray() ?: return false

        return SecurityUtils.verifyPassword(password, salt, hash)
    }

    // Session operations
    suspend fun createSession(deviceId: String, accessMethod: AccessMethod): AccessSession {
        val sessionId = SecurityUtils.generateSessionId()
        val expiresAt = LocalDateTime.now().plusMinutes(30) // 30 minute session

        val session = AccessSession(
            sessionId = sessionId,
            accessMethod = accessMethod,
            deviceId = deviceId,
            expiresAt = expiresAt,
            isTrustedDevice = false
        )

        database.accessSessionDao().insertSession(session)
        return session
    }

    fun getActiveSession(sessionId: String): Flow<AccessSession?> {
        return database.accessSessionDao().getSession(sessionId)
    }

    suspend fun updateSessionActivity(sessionId: String, duration: Long) {
        database.accessSessionDao().updateActivity(
            sessionId = sessionId,
            lastActivityAt = LocalDateTime.now(),
            duration = duration
        )
    }

    suspend fun deactivateSession(sessionId: String) {
        database.accessSessionDao().deactivateSession(sessionId)
    }

    // Attempt tracking
    suspend fun recordAccessAttempt(
        accessMethod: AccessMethod,
        status: AccessStatus,
        deviceId: String,
        sessionId: String? = null,
        errorCode: Int? = null,
        errorMessage: String? = null,
        attemptDurationMs: Long = 0
    ) {
        val attempt = AccessAttempt(
            accessMethod = accessMethod,
            status = status,
            deviceId = deviceId,
            sessionId = sessionId,
            errorCode = errorCode,
            errorMessage = errorMessage,
            attemptDurationMs = attemptDurationMs
        )

        database.accessAttemptDao().insertAttempt(attempt)
    }

    fun getRecentFailedAttempts(deviceId: String, limit: Int = 10): Flow<List<AccessAttempt>> {
        return database.accessAttemptDao().getRecentAttempts(deviceId, limit)
            .map { attempts -> attempts.filter { it.status != AccessStatus.SUCCESS } }
    }

    // Combined operations
    fun getSecurityStatus(): Flow<SecurityStatus> {
        return combine(
            getSecuritySettings(),
            getAccessCredential()
        ) { settings, credential ->
            SecurityStatus(
                isEnabled = settings?.isEnabled ?: false,
                accessMethod = settings?.accessMethod ?: AccessMethod.NONE,
                hasCredentials = credential != null && credential.isActive,
                isLocked = false, // TODO: Implement lockout logic
                lastAccessAt = settings?.lastAccessAt
            )
        }
    }

    // Cleanup operations
    suspend fun cleanupExpiredSessions() {
        val expiredSessions = database.accessSessionDao().getExpiredSessions(LocalDateTime.now())
        expiredSessions.forEach { session ->
            database.accessSessionDao().deactivateSession(session.sessionId)
        }
    }

    suspend fun cleanupOldAttempts(daysToKeep: Int = 30) {
        val cutoffDate = LocalDateTime.now().minusDays(daysToKeep.toLong())
        database.accessAttemptDao().cleanupOldAttempts(cutoffDate)
    }

    companion object {
        @Volatile
        private var INSTANCE: SecurityAccessRepository? = null

        fun getInstance(context: Context): SecurityAccessRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityAccessRepository(SecurityAccessDatabase.getInstance(context))
                    .also { INSTANCE = it }
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }

    data class SecurityStatus(
        val isEnabled: Boolean,
        val accessMethod: AccessMethod,
        val hasCredentials: Boolean,
        val isLocked: Boolean,
        val lastAccessAt: LocalDateTime?
    )
}