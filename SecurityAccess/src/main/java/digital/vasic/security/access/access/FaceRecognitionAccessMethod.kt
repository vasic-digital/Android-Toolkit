package digital.vasic.security.access.access

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import digital.vasic.security.access.data.AccessMethod
import digital.vasic.security.access.data.AccessStatus
import digital.vasic.security.access.data.BiometricType
import digital.vasic.security.access.data.SecurityAccessRepository
import digital.vasic.security.access.ui.FaceAccessActivity
import digital.vasic.security.access.utils.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class FaceRecognitionAccessMethod(
    private val priority: Int,
    private val context: AppCompatActivity,
    private val repository: SecurityAccessRepository = SecurityAccessRepository.getInstance(context)
) : BaseAccessMethod(priority, context) {

    override val accessMethod: AccessMethod = AccessMethod.FACE_RECOGNITION

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun checkCapability(callback: digital.vasic.security.access.utils.CapabilityCheckCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val biometricManager = BiometricManager.from(context)
                val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

                val settings = repository.getSecuritySettingsSync()
                val faceEnabled = settings?.allowFaceRecognition ?: true

                val capable = canAuthenticate && faceEnabled

                withContext(Dispatchers.Main) {
                    callback.onCapabilityChecked(capable)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onCapabilityChecked(false)
                }
            }
        }
    }

    override fun execute() {
        setupBiometricPrompt()
        biometricPrompt.authenticate(promptInfo)
    }

    override fun install() {
        // Face recognition doesn't require additional installation beyond hardware availability
        installationCallback.onInstallationChecked(true)
    }

    override fun checkInstalled(callback: digital.vasic.security.access.installation.InstallationCheckCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val biometricManager = BiometricManager.from(context)
                val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

                withContext(Dispatchers.Main) {
                    callback.onInstallationChecked(canAuthenticate)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onInstallationChecked(false)
                }
            }
        }
    }

    private fun setupBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(context)

        biometricPrompt = BiometricPrompt(context as androidx.fragment.app.FragmentActivity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)

                    val status = when (errorCode) {
                        BiometricPrompt.ERROR_HW_UNAVAILABLE -> AccessStatus.HARDWARE_UNAVAILABLE
                        BiometricPrompt.ERROR_UNABLE_TO_PROCESS -> AccessStatus.BIOMETRIC_NOT_AVAILABLE
                        BiometricPrompt.ERROR_TIMEOUT -> AccessStatus.TIMEOUT
                        BiometricPrompt.ERROR_NO_SPACE -> AccessStatus.BIOMETRIC_NOT_ENROLLED
                        BiometricPrompt.ERROR_CANCELED -> AccessStatus.USER_CANCELLED
                        BiometricPrompt.ERROR_LOCKOUT -> AccessStatus.BIOMETRIC_LOCKED_OUT
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> AccessStatus.BIOMETRIC_LOCKED_OUT
                        BiometricPrompt.ERROR_USER_CANCELED -> AccessStatus.USER_CANCELLED
                        BiometricPrompt.ERROR_NO_BIOMETRICS -> AccessStatus.BIOMETRIC_NOT_ENROLLED
                        BiometricPrompt.ERROR_HW_NOT_PRESENT -> AccessStatus.HARDWARE_UNAVAILABLE
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> AccessStatus.USER_CANCELLED
                        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> AccessStatus.BIOMETRIC_NOT_AVAILABLE
                        else -> AccessStatus.FAILED
                    }

                    recordAccessAttempt(status, errorCode, errString.toString())
                    executionCallback.onExecution(false, "onAuthenticationError: $errorCode")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()

                    recordAccessAttempt(AccessStatus.FAILED)
                    executionCallback.onExecution(false, "onAuthenticationFailed")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    recordAccessAttempt(AccessStatus.SUCCESS)
                    executionCallback.onExecution(true, "onAuthenticationSucceeded")
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Face Recognition")
            .setSubtitle("Use face recognition to access the app")
            .setDescription("Position your face within the frame")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    }

    private fun recordAccessAttempt(status: AccessStatus, errorCode: Int? = null, errorMessage: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deviceId = SecurityUtils.generateDeviceId(context)
                repository.recordAccessAttempt(
                    accessMethod = AccessMethod.FACE_RECOGNITION,
                    biometricType = BiometricType.FACE,
                    status = status,
                    deviceId = deviceId,
                    errorCode = errorCode,
                    errorMessage = errorMessage,
                    attemptDurationMs = 0 // Will be calculated by caller
                )
            } catch (e: Exception) {
                // Ignore errors in recording attempts
            }
        }
    }

    /**
     * Setup face recognition access for the first time
     */
    suspend fun setupFaceRecognition(): SetupResult {
        return withContext(Dispatchers.IO) {
            try {
                val biometricManager = BiometricManager.from(context)
                val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

                if (!canAuthenticate) {
                    return@withContext SetupResult.Error("Face recognition not available on this device")
                }

                // Check if face is enrolled
                val enrolled = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
                if (!enrolled) {
                    return@withContext SetupResult.Error("No face enrolled. Please enroll face in device settings first.")
                }

                // Update settings to enable face recognition access
                val settings = repository.getSecuritySettingsSync()?.copy(
                    accessMethod = AccessMethod.FACE_RECOGNITION,
                    biometricType = BiometricType.FACE,
                    isEnabled = true,
                    allowFaceRecognition = true,
                    updatedAt = LocalDateTime.now()
                ) ?: digital.vasic.security.access.data.SecuritySettings(
                    accessMethod = AccessMethod.FACE_RECOGNITION,
                    biometricType = BiometricType.FACE,
                    isEnabled = true,
                    allowFaceRecognition = true
                )

                repository.saveSecuritySettings(settings)

                SetupResult.Success
            } catch (e: Exception) {
                SetupResult.Error("Failed to setup face recognition: ${e.message}")
            }
        }
    }

    companion object {
        const val FACE_ACCESS_REQUEST_CODE = 1004
    }

    sealed class SetupResult {
        object Success : SetupResult()
        data class Error(val message: String) : SetupResult()
    }
}