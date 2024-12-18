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
