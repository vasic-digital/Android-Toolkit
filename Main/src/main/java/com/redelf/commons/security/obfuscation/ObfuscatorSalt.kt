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


package com.redelf.commons.security.obfuscation

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

data class ObfuscatorSalt(

    @SerializedName("identifier")
    @JsonProperty("identifier")
    var identifier: UUID? = UUID.randomUUID(),

    @SerializedName("value")
    @JsonProperty("value")
    private var value: String? = "",

    @SerializedName("error")
    @JsonProperty("error")
    var error: Throwable? = null,

    @JsonProperty("isFirstTimeObtained")
    @SerializedName("isFirstTimeObtained")
    val firstTimeObtained: AtomicBoolean = AtomicBoolean(),

    @JsonProperty("refreshCount")
    @SerializedName("refreshCount")
    val refreshCount: AtomicInteger = AtomicInteger(),

    @JsonProperty("refreshSkipCount")
    @SerializedName("refreshSkipCount")
    val refreshSkipCount: AtomicInteger = AtomicInteger()

) {

    override fun hashCode(): Int {

        return value.hashCode()
    }

    fun fromCache() = !firstTimeObtained.get()

    fun getTotalRefreshCount() = refreshCount.get() + refreshSkipCount.get()

    fun takeValue(): String = value ?: ""

    fun updateValue(newValue: String = value ?: ""): Int {

        if (value != newValue) {

            value = newValue

            return refreshCount.incrementAndGet()
        }

        return refreshSkipCount.incrementAndGet()
    }

    override fun equals(other: Any?): Boolean {

        if (this === other) return true

        if (javaClass != other?.javaClass) return false

        other as ObfuscatorSalt

        return value == other.value
    }
}
