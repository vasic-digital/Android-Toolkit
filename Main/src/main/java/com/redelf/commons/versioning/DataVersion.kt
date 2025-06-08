package com.redelf.commons.versioning

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import java.util.concurrent.atomic.AtomicLong

abstract class DataVersion(

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