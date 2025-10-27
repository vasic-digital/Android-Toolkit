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
import androidx.room.Query
import digital.vasic.security.access.data.AccessAttempt
import digital.vasic.security.access.data.AccessStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface AccessAttemptDao {

    @Insert
    suspend fun insertAttempt(attempt: AccessAttempt): Long

    @Query("SELECT * FROM access_attempts WHERE id = :id")
    fun getAttempt(id: Long): Flow<AccessAttempt?>

    @Query("SELECT * FROM access_attempts WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getAttemptsBySession(sessionId: String): Flow<List<AccessAttempt>>

    @Query("SELECT * FROM access_attempts WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAttempts(deviceId: String, limit: Int = 50): Flow<List<AccessAttempt>>

    @Query("SELECT COUNT(*) FROM access_attempts WHERE status = :status AND timestamp >= :since")
    suspend fun getFailedAttemptsCount(status: AccessStatus, since: LocalDateTime): Int

    @Query("SELECT COUNT(*) FROM access_attempts WHERE isSuspicious = 1 AND timestamp >= :since")
    suspend fun getSuspiciousAttemptsCount(since: LocalDateTime): Int

    @Query("SELECT * FROM access_attempts WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getAttemptsSince(since: LocalDateTime): Flow<List<AccessAttempt>>

    @Query("SELECT DISTINCT deviceId FROM access_attempts WHERE timestamp >= :since")
    suspend fun getActiveDevices(since: LocalDateTime): List<String>

    @Query("SELECT * FROM access_attempts WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND timestamp >= :since ORDER BY timestamp DESC LIMIT :limit")
    fun getAttemptsWithLocation(since: LocalDateTime, limit: Int = 100): Flow<List<AccessAttempt>>

    @Query("DELETE FROM access_attempts WHERE timestamp < :before")
    suspend fun cleanupOldAttempts(before: LocalDateTime)

    @Query("SELECT COUNT(*) FROM access_attempts")
    suspend fun getTotalCount(): Int
}