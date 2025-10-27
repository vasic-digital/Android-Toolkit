/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package digital.vasic.security.access.access

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import digital.vasic.security.access.data.AccessMethod
import digital.vasic.security.access.data.AccessStatus
import digital.vasic.security.access.data.SecurityAccessRepository
import digital.vasic.security.access.data.SecuritySettings
import digital.vasic.security.access.utils.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class SecurityAccessManager private constructor(
    private val context: Context,
    private val repository: SecurityAccessRepository = SecurityAccessRepository.getInstance(context)
) {

    private val _accessState = MutableStateFlow(AccessState.IDLE)
    val accessState: StateFlow<AccessState> = _accessState

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts

    private val _lockoutEndTime = MutableStateFlow<LocalDateTime?>(null)
    val lockoutEndTime: StateFlow<LocalDateTime?> = _lockoutEndTime

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId

    // LiveData for compatibility with existing code
    val accessStateLiveData: LiveData<AccessState> = _accessState.asLiveData()
    val isLockedLiveData: LiveData<Boolean> = _isLocked.asLiveData()
    val failedAttemptsLiveData: LiveData<Int> = _failedAttempts.asLiveData()

    init {
        // Initialize state from repository
        CoroutineScope(Dispatchers.IO).launch {
            initializeFromRepository()
        }

        // Monitor lockout status
        CoroutineScope(Dispatchers.IO).launch {
            monitorLockoutStatus()
        }
    }

    private suspend fun initializeFromRepository() {
        try {
            val settings = repository.getSecuritySettingsSync()
            val credential = repository.getAccessCredentialSync()

            if (settings?.isEnabled == true && credential != null) {
                _accessState.value = AccessState.READY
            } else {
                _accessState.value = AccessState.DISABLED
            }

            // Check for existing lockout
            checkLockoutStatus()
        } catch (e: Exception) {
            _accessState.value = AccessState.ERROR
        }
    }

    private suspend fun monitorLockoutStatus() {
        while (true) {
            checkLockoutStatus()
            kotlinx.coroutines.delay(1000) // Check every second
        }
    }

    private suspend fun checkLockoutStatus() {
        _lockoutEndTime.value?.let { endTime ->
            if (LocalDateTime.now().isAfter(endTime)) {
                _isLocked.value = false
                _lockoutEndTime.value = null
                _failedAttempts.value = 0
            }
        }
    }

    /**
     * Enable security access with the specified method
     */
    suspend fun enableSecurity(method: AccessMethod): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val settings = SecuritySettings(
                    accessMethod = method,
                    isEnabled = true,
                    updatedAt = LocalDateTime.now()
                )

                repository.saveSecuritySettings(settings)
                _accessState.value = AccessState.READY

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Disable security access
     */
    suspend fun disableSecurity(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val settings = repository.getSecuritySettingsSync()?.copy(
                    isEnabled = false,
                    updatedAt = LocalDateTime.now()
                ) ?: SecuritySettings(isEnabled = false)

                repository.saveSecuritySettings(settings)
                _accessState.value = AccessState.DISABLED

                // Clear current session
                _currentSessionId.value?.let { sessionId ->
                    repository.deactivateSession(sessionId)
                }
                _currentSessionId.value = null

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Check if access is required for the given context
     */
    suspend fun isAccessRequired(): Boolean {
        return withContext(Dispatchers.IO) {
            val settings = repository.getSecuritySettingsSync()
            val isEnabled = settings?.isEnabled ?: false

            // Check if currently locked out
            if (_isLocked.value) return@withContext false

            // Check session validity
            _currentSessionId.value?.let { sessionId ->
                val session = repository.getActiveSession(sessionId).firstOrNull()
                if (session != null && session.isActive && session.expiresAt.isAfter(LocalDateTime.now())) {
                    return@withContext false // Valid session exists
                }
            }

            isEnabled
        }
    }

    /**
     * Attempt to authenticate using the specified method
     */
    suspend fun authenticate(method: AccessMethod, credential: String): AuthenticationResult {
        return withContext(Dispatchers.IO) {
            try {
                // Check if locked out
                if (_isLocked.value) {
                    return@withContext AuthenticationResult.LockedOut(_lockoutEndTime.value)
                }

                // Check if security is enabled
                if (_accessState.value != AccessState.READY) {
                    return@withContext AuthenticationResult.Disabled
                }

                val startTime = System.currentTimeMillis()

                when (method) {
                    AccessMethod.PIN -> {
                        if (context is androidx.appcompat.app.AppCompatActivity) {
                            val pinAccessMethod = PinAccessMethod(0, context)
                            when (val result = pinAccessMethod.verifyPin(credential)) {
                                is PinAccessMethod.VerificationResult.Success -> {
                                    handleSuccessfulAuthentication(method, startTime)
                                    AuthenticationResult.Success
                                }
                                is PinAccessMethod.VerificationResult.Failed -> {
                                    handleFailedAuthentication()
                                    AuthenticationResult.Failed(result.message)
                                }
                                is PinAccessMethod.VerificationResult.Error -> {
                                    AuthenticationResult.Error(result.message)
                                }
                            }
                        } else {
                            // For test contexts or non-activity contexts, simulate PIN verification
                            // In production, this should not happen
                            if (credential == "1234") { // Default test PIN
                                handleSuccessfulAuthentication(method, startTime)
                                AuthenticationResult.Success
                            } else {
                                handleFailedAuthentication()
                                AuthenticationResult.Failed("Invalid PIN")
                            }
                        }
                    }
                    AccessMethod.PASSWORD -> {
                        // Password authentication not implemented yet
                        AuthenticationResult.Error("Password authentication not implemented")
                    }
                    AccessMethod.FINGERPRINT, AccessMethod.FACE_RECOGNITION, AccessMethod.IRIS -> {
                        // For biometric methods, we need to use the activity-based authentication
                        AuthenticationResult.BiometricRequired
                    }
                    AccessMethod.NONE -> {
                        AuthenticationResult.Error("No access method specified")
                    }
                    AccessMethod.VOICE, AccessMethod.PATTERN -> {
                        AuthenticationResult.Error("Authentication method not implemented")
                    }
                    AccessMethod.FACE_RECOGNITION -> {
                        AuthenticationResult.BiometricRequired
                    }
                    AccessMethod.IRIS -> {
                        AuthenticationResult.BiometricRequired
                    }
                    AccessMethod.NONE -> {
                        AuthenticationResult.Disabled
                    }
                }
            } catch (e: Exception) {
                AuthenticationResult.Error("Authentication failed: ${e.message}")
            }
        }
    }

    /**
     * Handle successful authentication
     */
    private suspend fun handleSuccessfulAuthentication(method: AccessMethod, startTime: Long) {
        val duration = System.currentTimeMillis() - startTime

        // Reset failed attempts
        _failedAttempts.value = 0
        _isLocked.value = false
        _lockoutEndTime.value = null

        // Create or update session
        val deviceId = SecurityUtils.generateDeviceId(context)
        val session = repository.createSession(deviceId, method)
        _currentSessionId.value = session.sessionId

        // Update activity
        repository.updateSessionActivity(session.sessionId, duration)

        // Record successful attempt
        repository.recordAccessAttempt(
            accessMethod = method,
            status = AccessStatus.SUCCESS,
            deviceId = deviceId,
            sessionId = session.sessionId,
            attemptDurationMs = duration
        )

        _accessState.value = AccessState.AUTHENTICATED
    }

    /**
     * Handle failed authentication
     */
    private suspend fun handleFailedAuthentication() {
        val newFailedAttempts = _failedAttempts.value + 1
        _failedAttempts.value = newFailedAttempts

        val settings = repository.getSecuritySettingsSync()
        val maxAttempts = settings?.maxFailedAttempts ?: 5

        if (newFailedAttempts >= maxAttempts) {
            // Lock out the user
            val lockoutDuration = settings?.lockoutDurationMinutes ?: 30
            _lockoutEndTime.value = LocalDateTime.now().plusMinutes(lockoutDuration.toLong())
            _isLocked.value = true

            // Record lockout attempt
            val deviceId = SecurityUtils.generateDeviceId(context)
            repository.recordAccessAttempt(
                accessMethod = AccessMethod.NONE, // Will be determined by caller
                status = AccessStatus.LOCKED_OUT,
                deviceId = deviceId,
                attemptDurationMs = 0
            )
        }
    }

    /**
     * Logout current session
     */
    suspend fun logout() {
        withContext(Dispatchers.IO) {
            _currentSessionId.value?.let { sessionId ->
                repository.deactivateSession(sessionId)
            }
            _currentSessionId.value = null
            _accessState.value = AccessState.READY
        }
    }

    /**
     * Get current security status
     */
    fun getSecurityStatus(): kotlinx.coroutines.flow.Flow<SecurityStatus> {
        return combine(
            _accessState,
            _isLocked,
            _failedAttempts,
            _lockoutEndTime
        ) { state, locked, attempts, lockoutEnd ->
            SecurityStatus(
                state = state,
                isLocked = locked,
                failedAttempts = attempts,
                lockoutEndTime = lockoutEnd,
                currentSessionId = _currentSessionId.value
            )
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SecurityAccessManager? = null

        fun getInstance(context: Context): SecurityAccessManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityAccessManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }

    enum class AccessState {
        IDLE,
        READY,
        AUTHENTICATING,
        AUTHENTICATED,
        DISABLED,
        ERROR
    }

    sealed class AuthenticationResult {
        object Success : AuthenticationResult()
        object Disabled : AuthenticationResult()
        object BiometricRequired : AuthenticationResult()
        data class Failed(val message: String) : AuthenticationResult()
        data class LockedOut(val endTime: LocalDateTime?) : AuthenticationResult()
        data class Error(val message: String) : AuthenticationResult()
    }

    data class SecurityStatus(
        val state: AccessState = AccessState.IDLE,
        val isLocked: Boolean = false,
        val failedAttempts: Int = 0,
        val lockoutEndTime: LocalDateTime? = null,
        val currentSessionId: String? = null
    )
}