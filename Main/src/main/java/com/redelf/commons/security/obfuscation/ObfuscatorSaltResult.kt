package com.redelf.commons.security.obfuscation

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

data class RemoteSaltResult(

    @SerializedName("salt")
    @JsonProperty("salt")
    var salt: String? = "",

    @SerializedName("error")
    @JsonProperty("error")
    var error: Throwable? = null,

    @JsonProperty("isFirstTimeObtained")
    @SerializedName("isFirstTimeObtained")
    var isFirstTimeObtained: Boolean = false
)
