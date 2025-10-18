package digital.vasic.security.access.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import digital.vasic.security.access.data.AccessCredential
import kotlinx.coroutines.flow.Flow

@Dao
interface AccessCredentialDao {

    @Query("SELECT * FROM access_credentials WHERE id = :id")
    fun getCredential(id: String = "default"): Flow<AccessCredential?>

    @Query("SELECT * FROM access_credentials WHERE id = :id")
    suspend fun getCredentialSync(id: String = "default"): AccessCredential?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: AccessCredential)

    @Update
    suspend fun updateCredential(credential: AccessCredential)

    @Query("UPDATE access_credentials SET hashedPin = :hashedPin, salt = :salt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePin(id: String = "default", hashedPin: String?, salt: String?, updatedAt: java.time.LocalDateTime)

    @Query("UPDATE access_credentials SET hashedPassword = :hashedPassword, salt = :salt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePassword(id: String = "default", hashedPassword: String?, salt: String?, updatedAt: java.time.LocalDateTime)

    @Query("UPDATE access_credentials SET biometricTemplate = :template, biometricType = :type, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBiometric(id: String = "default", template: ByteArray?, type: digital.vasic.security.access.data.BiometricType?, updatedAt: java.time.LocalDateTime)

    @Query("UPDATE access_credentials SET lastUsedAt = :lastUsedAt WHERE id = :id")
    suspend fun updateLastUsed(id: String = "default", lastUsedAt: java.time.LocalDateTime)

    @Query("UPDATE access_credentials SET isActive = :active WHERE id = :id")
    suspend fun updateActiveStatus(id: String = "default", active: Boolean)

    @Query("UPDATE access_credentials SET isCompromised = :compromised WHERE id = :id")
    suspend fun markCompromised(id: String = "default", compromised: Boolean)

    @Query("SELECT COUNT(*) FROM access_credentials WHERE isActive = 1")
    suspend fun getActiveCount(): Int

    @Query("DELETE FROM access_credentials WHERE id = :id")
    suspend fun deleteCredential(id: String)
}