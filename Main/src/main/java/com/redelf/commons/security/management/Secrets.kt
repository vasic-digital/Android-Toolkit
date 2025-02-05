package com.redelf.commons.security.management

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.redelf.commons.security.obfuscation.ObfuscatorSalt

data class Secrets(

    @SerializedName("obfuscationSalt")
    @JsonProperty("obfuscationSalt")
    var obfuscationSalt: ObfuscatorSalt? = null
)
