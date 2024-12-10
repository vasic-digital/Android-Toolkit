package com.redelf.commons.referring.implementation.facebook

import com.fasterxml.jackson.annotation.JsonProperty
import com.redelf.commons.referring.implementation.InstallReferrerData
import kotlinx.serialization.SerialName

class FacebookInstallReferrerData(

    @JsonProperty("mir")
    @SerialName("mir")
    var mir: String? = "",

    timestamp: Long? = System.currentTimeMillis()

) : InstallReferrerData(timestamp)
