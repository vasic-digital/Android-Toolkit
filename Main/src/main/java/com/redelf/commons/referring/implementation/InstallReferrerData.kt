package com.redelf.commons.referring.implementation

import com.fasterxml.jackson.annotation.JsonProperty
import com.redelf.commons.expiration.ExpirationParametrized
import com.redelf.commons.extensions.toDays
import kotlinx.serialization.SerialName
import kotlin.math.abs

abstract class InstallReferrerData(

    @SerialName("timestamp")
    @JsonProperty("timestamp")
    var timestamp: Long? = System.currentTimeMillis()

) : ExpirationParametrized<Int> {

    override fun isExpired(days: Int): Boolean {

        timestamp?.let { time ->

            val timeDiff = abs(System.currentTimeMillis() - time)

            return timeDiff.toDays() >= days
        }

        return false
    }
}