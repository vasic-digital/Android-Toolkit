package com.redelf.commons.test.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.redelf.commons.logging.Timber
import com.redelf.commons.partition.Partitional
import java.lang.reflect.Type
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class SampleDataOnlyP3 @JsonCreator constructor(

    @JsonProperty("partitioningOn")
    @SerializedName("partitioningOn")
    private val partitioningOn: Boolean = true,

    @JsonProperty("partition3")
    @SerializedName("partition3")
    var partition3: ConcurrentHashMap<String, List<SampleData3>> = ConcurrentHashMap(),

) : Partitional<SampleDataOnlyP3> {

    constructor() : this(

        partitioningOn = true
    )

    override fun getClazz(): Class<SampleDataOnlyP3> {

        return SampleDataOnlyP3::class.java
    }

    override fun isPartitioningEnabled() = partitioningOn

    fun isPartitioningDisabled() = !partitioningOn

    override fun getPartitionCount() = 1

    override fun getPartitionData(number: Int): Any? {

        return when (number) {

            0 -> partition3

            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun setPartitionData(number: Int, data: Any?): Boolean {

        if (data == null) {

            return true
        }

        when (number) {

            0 -> {

                try {

                    partition3 = ConcurrentHashMap()
                    partition3.putAll(data as ConcurrentHashMap<String, List<SampleData3>>)

                } catch (e: Exception) {

                    Timber.e(e)

                    return false
                }

                return true
            }

            else -> return false
        }
    }

    override fun getPartitionType(number: Int): Type? {

        return when (number) {

            0 -> object : TypeToken<ConcurrentHashMap<String, List<SampleData3>>>() {}.type

            else -> null
        }
    }
}
