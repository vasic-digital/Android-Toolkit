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