package com.redelf.commons.test.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.redelf.commons.logging.Timber
import com.redelf.commons.partition.Partitional
import java.lang.reflect.Type

data class SampleDataOnlyP5 @JsonCreator constructor(

    @JsonProperty("partitioningOn")
    @SerializedName("partitioningOn")
    private val partitioningOn: Boolean = true,

    @JsonProperty("partition5")
    @SerializedName("partition5")
    var partition5: String = "",

) : Partitional<SampleDataOnlyP5> {

    constructor() : this(

        partitioningOn = true
    )

    override fun getClazz(): Class<SampleDataOnlyP5> {

        return SampleDataOnlyP5::class.java
    }

    override fun isPartitioningEnabled() = partitioningOn

    fun isPartitioningDisabled() = !partitioningOn

    override fun isPartitioningParallelized() = true

    override fun getPartitionCount() = 1

    override fun getPartitionData(number: Int): Any? {

        return when (number) {

            0 -> partition5

            else -> null
        }
    }

    override fun setPartitionData(number: Int, data: Any?): Boolean {

        if (data == null) {

            return true
        }

        when (number) {

            0 -> {

                try {

                    partition5 = data as String

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

            0 -> object : TypeToken<String>() {}.type

            else -> null
        }
    }
}
