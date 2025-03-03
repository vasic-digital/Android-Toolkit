package com.redelf.commons.test.test_data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.redelf.commons.logging.Console
import com.redelf.commons.partition.Partitioning
import java.lang.reflect.Type
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class SampleDataOnlyP2 @JsonCreator constructor(

    @JsonProperty("partitioningOn")
    @SerializedName("partitioningOn")
    private val partitioningOn: Boolean = true,

    @JsonProperty("partition2")
    @SerializedName("partition2")
    var partition2: ConcurrentHashMap<UUID, SampleData2> = ConcurrentHashMap(),

) : Partitioning<SampleDataOnlyP2> {

    constructor() : this(

        partitioningOn = true
    )

    override fun failPartitionData(number: Int, error: Throwable) {

        Console.error(error)
    }

    override fun getClazz(): Class<SampleDataOnlyP2> {

        return SampleDataOnlyP2::class.java
    }

    override fun isPartitioningEnabled() = partitioningOn

    fun isPartitioningDisabled() = !partitioningOn

    override fun isPartitioningParallelized() = true

    override fun getPartitionCount() = 1

    override fun getPartitionData(number: Int): Any? {

        return when (number) {

            0 -> partition2

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

                    partition2 = ConcurrentHashMap()
                    partition2.putAll(data as ConcurrentHashMap<UUID, SampleData2>)

                } catch (e: Exception) {

                    Console.error(e)

                    return false
                }

                return true
            }

            else -> return false
        }
    }

    override fun getPartitionType(number: Int): Type? {

        return when (number) {

            0 -> object : TypeToken<ConcurrentHashMap<UUID, SampleData2>>() {}.type

            else -> null
        }
    }
}
