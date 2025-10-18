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