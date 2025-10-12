package com.redelf.commons.data.wrapper

import com.google.gson.annotations.SerializedName
import com.redelf.commons.versioning.Versionable
import java.util.concurrent.atomic.AtomicLong

open class VersionableWrapper<T>(data: T) : Wrapper<T>(data), Versionable {

    @SerializedName("data_version")
    private val dataVersion: AtomicLong = AtomicLong()

    override fun getVersion(): Long {

        return dataVersion.get()
    }

    override fun increaseVersion(): Long {

        return dataVersion.incrementAndGet()
    }
}