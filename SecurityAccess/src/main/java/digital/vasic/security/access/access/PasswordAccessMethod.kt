package digital.vasic.security.access.access

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import digital.vasic.security.access.data.AccessMethod
import digital.vasic.security.access.data.AccessStatus
import digital.vasic.security.access.data.SecurityAccessRepository
import digital.vasic.security.access.ui.PasswordAccessActivity
import digital.vasic.security.access.utils.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class PasswordAccessMethod(
    private val priority: Int,
    private val context: AppCompatActivity,
    private val repository: SecurityAccessRepository = SecurityAccessRepository.getInstance(context)
) : BaseAccessMethod(priority, context) {

    override val accessMethod: AccessMethod = AccessMethod.PASSWORD

    override fun checkCapability(callback: digital.vasic.security.access.utils.CapabilityCheckCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = repository.getSecuritySettingsSync()
                val hasPassword = settings?.accessMethod == AccessMethod.PASSWORD

                withContext(Dispatchers.Main) {
                    callback.onCapabilityChecked(hasPassword)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onCapabilityChecked(false)
                }
            }
        }
    }

    override fun execute() {
        val intent = android.content.Intent(context, PasswordAccessActivity::class.java)
        context.startActivityForResult(intent, PASSWORD_ACCESS_REQUEST_CODE)
    }

    override fun install() {
        // Password access doesn't require additional installation beyond credential setup
        installationCallback.onInstallationChecked(true)
    }

    override fun checkInstalled(callback: digital.vasic.security.access.installation.InstallationCheckCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val credential = repository.getAccessCredentialSync()
                val isInstalled = credential?.hashedPassword != null && credential.isActive

                withContext(Dispatchers.Main) {
                    callback.onInstallationChecked(isInstalled)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onInstallationChecked(false)
                }
            }
        }
    }

    /**
     * Setup password for the first time
     */
    suspend fun setupPassword(password: String, confirmPassword: String): SetupResult {
        return withContext(Dispatchers.IO) {
            try {
                // Validate password
                if (password != confirmPassword) {
                    return@withContext SetupResult.Error("Passwords do not match")
                }

                // Validate password strength
                val settings = repository.getSecuritySettingsSync()
                val passwordValidation = SecurityUtils.validatePasswordStrength(
                    password = password,
                    minLength = settings?.passwordMinLength ?: 8,
                    requireUppercase = settings?.requireUppercase ?: false,
                    requireLowercase = true, // Always require lowercase for security
                    requireNumbers = settings?.requireNumbers ?: false,
                    requireSpecialChars = settings?.requireSpecialChars ?: false
                )

                if (!passwordValidation.isValid) {
                    return@withContext SetupResult.Error(passwordValidation.issues.joinToString(", "))
                }

                // Check if password already exists
                val existingCredential = repository.getAccessCredentialSync()
                if (existingCredential?.hashedPassword != null) {
                    return@withContext SetupResult.Error("Password already configured")
                }

                // Generate salt and hash password
                val salt = SecurityUtils.generateSalt()
                val hashedPassword = SecurityUtils.hashPassword(password, salt)

                // Create or update credential
                val credential = existingCredential?.copy(
                    hashedPassword = hashedPassword.joinToString("") { "%02x".format(it) },
                    salt = salt.joinToString("") { "%02x".format(it) },
                    updatedAt = LocalDateTime.now()
                ) ?: digital.vasic.security.access.data.AccessCredential(
                    hashedPassword = hashedPassword.joinToString("") { "%02x".format(it) },
                    salt = salt.joinToString("") { "%02x".format(it) }
                )

                repository.saveAccessCredential(credential)

                // Update settings to enable password access
                val updatedSettings = settings?.copy(
                    accessMethod = AccessMethod.PASSWORD,
                    isEnabled = true,
                    updatedAt = LocalDateTime.now()
                ) ?: digital.vasic.security.access.data.SecuritySettings(
                    accessMethod = AccessMethod.PASSWORD,
                    isEnabled = true
                )

                repository.saveSecuritySettings(updatedSettings)

                SetupResult.Success
            } catch (e: Exception) {
                SetupResult.Error("Failed to setup password: ${e.message}")
            }
        }
    }

    /**
     * Verify password during access attempt
     */
    suspend fun verifyPassword(password: String): VerificationResult {
        return withContext(Dispatchers.IO) {
            try {
                val isValid = repository.verifyPassword(password)

                if (isValid) {
                    // Record successful attempt
                    val deviceId = SecurityUtils.generateDeviceId(context)
                    repository.recordAccessAttempt(
                        accessMethod = AccessMethod.PASSWORD,
                        status = AccessStatus.SUCCESS,
                        deviceId = deviceId,
                        attemptDurationMs = 0 // Will be calculated by caller
                    )

                    // Update last access time
                    repository.getSecuritySettingsSync()?.let { settings ->
                        val updatedSettings = settings.copy(lastAccessAt = LocalDateTime.now())
                        repository.saveSecuritySettings(updatedSettings)
                    }

                    VerificationResult.Success
                } else {
                    // Record failed attempt
                    val deviceId = SecurityUtils.generateDeviceId(context)
                    repository.recordAccessAttempt(
                        accessMethod = AccessMethod.PASSWORD,
                        status = AccessStatus.FAILED,
                        deviceId = deviceId,
                        attemptDurationMs = 0
                    )

                    VerificationResult.Failed("Invalid password")
                }
            } catch (e: Exception) {
                VerificationResult.Error("Verification failed: ${e.message}")
            }
        }
    }

    /**
     * Change existing password
     */
    suspend fun changePassword(currentPassword: String, newPassword: String, confirmNewPassword: String): ChangeResult {
        return withContext(Dispatchers.IO) {
            try {
                // Verify current password
                val currentValid = repository.verifyPassword(currentPassword)
                if (!currentValid) {
                    return@withContext ChangeResult.Error("Current password is incorrect")
                }

                // Validate new password
                if (newPassword != confirmNewPassword) {
                    return@withContext ChangeResult.Error("New passwords do not match")
                }

                val settings = repository.getSecuritySettingsSync()
                val passwordValidation = SecurityUtils.validatePasswordStrength(
                    password = newPassword,
                    minLength = settings?.passwordMinLength ?: 8,
                    requireUppercase = settings?.requireUppercase ?: false,
                    requireLowercase = true,
                    requireNumbers = settings?.requireNumbers ?: false,
                    requireSpecialChars = settings?.requireSpecialChars ?: false
                )

                if (!passwordValidation.isValid) {
                    return@withContext ChangeResult.Error(passwordValidation.issues.joinToString(", "))
                }

                if (newPassword == currentPassword) {
                    return@withContext ChangeResult.Error("New password must be different from current password")
                }

                // Update password
                repository.updatePassword(newPassword)

                ChangeResult.Success
            } catch (e: Exception) {
                ChangeResult.Error("Failed to change password: ${e.message}")
            }
        }
    }

    companion object {
        const val PASSWORD_ACCESS_REQUEST_CODE = 1002
    }

    sealed class SetupResult {
        object Success : SetupResult()
        data class Error(val message: String) : SetupResult()
    }

    sealed class VerificationResult {
        object Success : VerificationResult()
        data class Failed(val message: String) : VerificationResult()
        data class Error(val message: String) : VerificationResult()
    }

    sealed class ChangeResult {
        object Success : ChangeResult()
        data class Error(val message: String) : ChangeResult()
    }
}