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