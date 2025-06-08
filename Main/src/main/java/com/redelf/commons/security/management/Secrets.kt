package com.redelf.commons.security.management

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.redelf.commons.security.obfuscation.ObfuscatorSalt
import com.redelf.commons.versioning.Versionable
import java.util.concurrent.atomic.AtomicLong

data class Secrets(

    @SerializedName("obfuscationSalt")
    @JsonProperty("obfuscationSalt")
    var obfuscationSalt: ObfuscatorSalt? = null,

    @SerializedName("dataVersion")
    @JsonProperty("dataVersion")
    private val dataVersion: AtomicLong = AtomicLong()

) : Versionable {

    override fun getVersion(): Long {

        return dataVersion.get()
    }

    override fun increaseVersion(): Long {

        return dataVersion.incrementAndGet()
    }
}
