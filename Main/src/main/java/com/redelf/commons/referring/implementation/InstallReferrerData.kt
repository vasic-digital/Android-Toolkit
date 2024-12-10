package com.redelf.commons.referring.implementation

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.SerialName

abstract class InstallReferrerData(

    @SerialName("timestamp")
    @JsonProperty("timestamp")
    var timestamp: Long? = System.currentTimeMillis()
)