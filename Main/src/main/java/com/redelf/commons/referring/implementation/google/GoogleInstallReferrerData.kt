package com.redelf.commons.referring.implementation.google

import com.fasterxml.jackson.annotation.JsonProperty
import com.redelf.commons.referring.implementation.InstallReferrerData
import kotlinx.serialization.SerialName

class GoogleInstallReferrerData(

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
    var googlePlayInstantParam: Boolean? = false,

    timestamp: Long? = System.currentTimeMillis()

) : InstallReferrerData(timestamp)
