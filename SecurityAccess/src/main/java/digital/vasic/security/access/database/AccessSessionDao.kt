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


package digital.vasic.security.access.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import digital.vasic.security.access.data.AccessSession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface AccessSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AccessSession): Long

    @Query("SELECT * FROM access_sessions WHERE sessionId = :sessionId")
    fun getSession(sessionId: String): Flow<AccessSession?>

    @Query("SELECT * FROM access_sessions WHERE deviceId = :deviceId AND isActive = 1 ORDER BY lastActivityAt DESC")
    fun getActiveSessions(deviceId: String): Flow<List<AccessSession>>

    @Query("SELECT * FROM access_sessions WHERE isActive = 1 ORDER BY lastActivityAt DESC")
    fun getAllActiveSessions(): Flow<List<AccessSession>>

    @Query("SELECT * FROM access_sessions WHERE expiresAt < :now")
    suspend fun getExpiredSessions(now: LocalDateTime): List<AccessSession>

    @Query("UPDATE access_sessions SET isActive = 0 WHERE sessionId = :sessionId")
    suspend fun deactivateSession(sessionId: String)

    @Query("UPDATE access_sessions SET isLocked = 1, lockReason = :reason, lockedAt = :lockedAt WHERE sessionId = :sessionId")
    suspend fun lockSession(sessionId: String, reason: String, lockedAt: LocalDateTime)

    @Query("UPDATE access_sessions SET isLocked = 0, lockReason = NULL, lockedAt = NULL WHERE sessionId = :sessionId")
    suspend fun unlockSession(sessionId: String)

    @Query("UPDATE access_sessions SET lastActivityAt = :lastActivityAt, accessCount = accessCount + 1, totalDurationMs = totalDurationMs + :duration WHERE sessionId = :sessionId")
    suspend fun updateActivity(sessionId: String, lastActivityAt: LocalDateTime, duration: Long)

    @Query("UPDATE access_sessions SET failedAttempts = failedAttempts + 1 WHERE sessionId = :sessionId")
    suspend fun incrementFailedAttempts(sessionId: String)

    @Query("DELETE FROM access_sessions WHERE expiresAt < :before")
    suspend fun cleanupExpiredSessions(before: LocalDateTime)

    @Query("SELECT COUNT(*) FROM access_sessions WHERE isActive = 1")
    suspend fun getActiveSessionCount(): Int

    @Query("SELECT COUNT(*) FROM access_sessions WHERE isTrustedDevice = 1")
    suspend fun getTrustedDeviceCount(): Int
}