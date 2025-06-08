package com.redelf.commons.messaging.firebase

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import com.google.gson.annotations.SerializedName
import com.redelf.commons.versioning.Versionable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class FirebaseConfiguration(

    @SerializedName("dataVersion")
    @JsonProperty("dataVersion")
    private val dataVersion: AtomicLong = AtomicLong()

) : ConcurrentHashMap<String, FirebaseRemoteConfigValue>(), Versionable {

    override fun getVersion(): Long {

        return dataVersion.get()
    }

    override fun increaseVersion(): Long {

        return dataVersion.incrementAndGet()
    }
}
