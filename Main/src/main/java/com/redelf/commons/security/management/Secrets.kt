package com.redelf.commons.security.management

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.redelf.commons.security.obfuscation.ObfuscatorSalt
import com.redelf.commons.versioning.Versionable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class Secrets(

    @SerializedName("obfuscationSalt")
    @JsonProperty("obfuscationSalt")
    var obfuscationSalt: ObfuscatorSalt? = null,

    @SerializedName("dataVersion")
    @JsonProperty("dataVersion")
    private val dataVersion: AtomicLong? = AtomicLong(),

    @JsonProperty("secrets")
    @SerializedName("secrets")
    var secrets: ConcurrentHashMap<String, String>? = ConcurrentHashMap()

) : Versionable {

    override fun getVersion(): Long {

        return dataVersion?.get() ?: 0
    }

    override fun increaseVersion(): Long {

        return dataVersion?.incrementAndGet() ?: 0
    }
}
