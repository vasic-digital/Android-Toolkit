package com.redelf.commons.security.management

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

data class Secrets(

    @SerializedName("obfuscationSalt")
    @JsonProperty("obfuscationSalt")
    var obfuscationSalt: String? = ""
)
