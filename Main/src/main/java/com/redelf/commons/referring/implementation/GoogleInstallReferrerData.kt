package com.redelf.commons.referring.implementation

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.SerialName

data class GoogleInstallReferrerData(

    @SerialName("referrerUrl")
    @JsonProperty("referrerUrl")
    var referrerUrl: String? = "",

    @SerialName("referrerClickTimestampSeconds")
    @JsonProperty("referrerClickTimestampSeconds")
    var referrerClickTimestampSeconds: Long? = 0,

    @SerialName("installBeginTimestampSeconds")
    @JsonProperty("installBeginTimestampSeconds")
    var installBeginTimestampSeconds: Long? = 0,

    @SerialName("googlePlayInstantParam")
    @JsonProperty("googlePlayInstantParam")
    var googlePlayInstantParam: Boolean? = false
)
