package digital.vasic.security.access.database

import androidx.room.TypeConverter
import digital.vasic.security.access.data.AccessMethod
import digital.vasic.security.access.data.AccessStatus
import digital.vasic.security.access.data.BiometricType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class SecurityAccessTypeConverters {

    @TypeConverter
    fun fromAccessMethod(value: AccessMethod): String {
        return value.name
    }

    @TypeConverter
    fun toAccessMethod(value: String): AccessMethod {
        return AccessMethod.valueOf(value)
    }

    @TypeConverter
    fun fromAccessStatus(value: AccessStatus): String {
        return value.name
    }

    @TypeConverter
    fun toAccessStatus(value: String): AccessStatus {
        return AccessStatus.valueOf(value)
    }

    @TypeConverter
    fun fromBiometricType(value: BiometricType): String {
        return value.name
    }

    @TypeConverter
    fun toBiometricType(value: String): BiometricType {
        return BiometricType.valueOf(value)
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) }
    }

    @TypeConverter
    fun toTimestamp(value: LocalDateTime?): Long? {
        return value?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }
}