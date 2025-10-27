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