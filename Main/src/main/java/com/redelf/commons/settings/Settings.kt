package com.redelf.commons.settings

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import java.util.concurrent.ConcurrentHashMap

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
)
