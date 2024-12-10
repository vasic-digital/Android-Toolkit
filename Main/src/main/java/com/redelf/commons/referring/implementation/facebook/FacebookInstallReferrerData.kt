package com.redelf.commons.referring.implementation.facebook

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.SerialName

data class FacebookInstallReferrerData(

    @JsonProperty("mir")
    @SerialName("mir")
    val mir: String
)
