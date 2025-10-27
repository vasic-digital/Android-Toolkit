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
import digital.vasic.security.access.data.SecuritySettings
import kotlinx.coroutines.flow.Flow

@Dao
interface SecuritySettingsDao {

    @Query("SELECT * FROM security_settings WHERE id = :id")
    fun getSettings(id: String = "default"): Flow<SecuritySettings?>

    @Query("SELECT * FROM security_settings WHERE id = :id")
    suspend fun getSettingsSync(id: String = "default"): SecuritySettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SecuritySettings)

    @Update
    suspend fun updateSettings(settings: SecuritySettings)

    @Query("UPDATE security_settings SET isEnabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateEnabledStatus(id: String = "default", enabled: Boolean, updatedAt: java.time.LocalDateTime)

    @Query("UPDATE security_settings SET lastAccessAt = :lastAccessAt WHERE id = :id")
    suspend fun updateLastAccess(id: String = "default", lastAccessAt: java.time.LocalDateTime)

    @Query("SELECT COUNT(*) FROM security_settings WHERE isEnabled = 1")
    suspend fun getEnabledCount(): Int

    @Query("DELETE FROM security_settings WHERE id = :id")
    suspend fun deleteSettings(id: String)
}