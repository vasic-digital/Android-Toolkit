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
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import digital.vasic.security.access.data.AccessMethod
import digital.vasic.security.access.data.AccessStatus
import digital.vasic.security.access.data.SecurityAccessRepository
// Removed UI activity import for simplified implementation
import digital.vasic.security.access.utils.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class PinAccessMethod(
    private val priority: Int,
    context: AppCompatActivity,
    private val repository: SecurityAccessRepository = SecurityAccessRepository.getInstance(context)
) : BaseAccessMethod(priority, context) {

    override val accessMethod: AccessMethod = AccessMethod.PIN

    override fun checkCapability(callback: digital.vasic.security.access.utils.CapabilityCheckCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = repository.getSecuritySettingsSync()
                val hasPin = settings?.accessMethod == AccessMethod.PIN

                withContext(Dispatchers.Main) {
                    callback.onCapabilityChecked(hasPin)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onCapabilityChecked(false)
                }
            }
        }
    }

    override fun execute() {
        // PIN authentication is handled directly by the consuming app
        // This method is not used in the simplified implementation
    }

    override fun install() {
        // PIN access doesn't require additional installation beyond credential setup
        installationCallback.onInstallationChecked(true)
    }

    override fun checkInstalled(callback: digital.vasic.security.access.installation.InstallationCheckCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val credential = repository.getAccessCredentialSync()
                val isInstalled = credential?.hashedPin != null && credential.isActive

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
     * Setup PIN for the first time
     */
    suspend fun setupPin(pin: String, confirmPin: String): SetupResult {
        return withContext(Dispatchers.IO) {
            try {
                // Validate PIN
                if (pin != confirmPin) {
                    return@withContext SetupResult.Error("PINs do not match")
                }

                if (pin.length < 4) {
                    return@withContext SetupResult.Error("PIN must be at least 4 digits")
                }

                // Check if PIN already exists
                val existingCredential = repository.getAccessCredentialSync()
                if (existingCredential?.hashedPin != null) {
                    return@withContext SetupResult.Error("PIN already configured")
                }

                // Generate salt and hash PIN
                val salt = SecurityUtils.generateSalt()
                val hashedPin = SecurityUtils.hashPin(pin, salt)

                // Create or update credential
                val credential = existingCredential?.copy(
                    hashedPin = hashedPin.joinToString("") { "%02x".format(it) },
                    salt = salt.joinToString("") { "%02x".format(it) },
                    updatedAt = LocalDateTime.now()
                ) ?: digital.vasic.security.access.data.AccessCredential(
                    hashedPin = hashedPin.joinToString("") { "%02x".format(it) },
                    salt = salt.joinToString("") { "%02x".format(it) }
                )

                repository.saveAccessCredential(credential)

                // Update settings to enable PIN access
                val settings = repository.getSecuritySettingsSync()?.copy(
                    accessMethod = AccessMethod.PIN,
                    isEnabled = true,
                    updatedAt = LocalDateTime.now()
                ) ?: digital.vasic.security.access.data.SecuritySettings(
                    accessMethod = AccessMethod.PIN,
                    isEnabled = true
                )

                repository.saveSecuritySettings(settings)

                SetupResult.Success
            } catch (e: Exception) {
                SetupResult.Error("Failed to setup PIN: ${e.message}")
            }
        }
    }

    /**
     * Verify PIN during access attempt
     */
    suspend fun verifyPin(pin: String): VerificationResult {
        return withContext(Dispatchers.IO) {
            try {
                val isValid = repository.verifyPin(pin)

                if (isValid) {
                    // Record successful attempt
                    val deviceId = SecurityUtils.generateDeviceId(context)
                    repository.recordAccessAttempt(
                        accessMethod = AccessMethod.PIN,
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
                        accessMethod = AccessMethod.PIN,
                        status = AccessStatus.FAILED,
                        deviceId = deviceId,
                        attemptDurationMs = 0
                    )

                    VerificationResult.Failed("Invalid PIN")
                }
            } catch (e: Exception) {
                VerificationResult.Error("Verification failed: ${e.message}")
            }
        }
    }

    /**
     * Change existing PIN
     */
    suspend fun changePin(currentPin: String, newPin: String, confirmNewPin: String): ChangeResult {
        return withContext(Dispatchers.IO) {
            try {
                // Verify current PIN
                val currentValid = repository.verifyPin(currentPin)
                if (!currentValid) {
                    return@withContext ChangeResult.Error("Current PIN is incorrect")
                }

                // Validate new PIN
                if (newPin != confirmNewPin) {
                    return@withContext ChangeResult.Error("New PINs do not match")
                }

                if (newPin.length < 4) {
                    return@withContext ChangeResult.Error("PIN must be at least 4 digits")
                }

                if (newPin == currentPin) {
                    return@withContext ChangeResult.Error("New PIN must be different from current PIN")
                }

                // Update PIN
                repository.updatePin(newPin)

                ChangeResult.Success
            } catch (e: Exception) {
                ChangeResult.Error("Failed to change PIN: ${e.message}")
            }
        }
    }

    companion object {
        const val PIN_ACCESS_REQUEST_CODE = 1001
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