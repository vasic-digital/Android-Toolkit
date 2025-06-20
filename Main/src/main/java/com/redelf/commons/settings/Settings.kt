package com.redelf.commons.settings

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.redelf.commons.versioning.Versionable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class Settings(

    @JsonProperty("flags")
    @SerializedName("flags")
    var flags: ConcurrentHashMap<String, Boolean>? = ConcurrentHashMap<String, Boolean>(),

    @JsonProperty("values")
    @SerializedName("values")
    var values: ConcurrentHashMap<String, String>? = ConcurrentHashMap<String, String>(),

    @JsonProperty("numbers")
    @SerializedName("numbers")
    var numbers: ConcurrentHashMap<String, Long>? = ConcurrentHashMap<String, Long>(),

    @SerializedName("dataVersion")
    @JsonProperty("dataVersion")
    private val dataVersion: AtomicLong = AtomicLong()

) : Versionable {

    override fun getVersion(): Long {

        return dataVersion.get()
    }

    override fun increaseVersion(): Long {

        return dataVersion.incrementAndGet()
    }
}
