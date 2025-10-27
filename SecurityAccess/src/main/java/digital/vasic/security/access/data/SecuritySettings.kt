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