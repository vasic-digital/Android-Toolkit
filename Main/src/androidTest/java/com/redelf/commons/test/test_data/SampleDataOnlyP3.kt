package com.redelf.commons.test.test_data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import com.redelf.commons.logging.Console
import com.redelf.commons.partition.Partitioning
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

data class SampleDataOnlyP3 @JsonCreator constructor(

    @JsonProperty("partitioningOn")
    @SerializedName("partitioningOn")
    private val partitioningOn: Boolean = true,

    @JsonProperty("partition3")
    @SerializedName("partition3")
    var partition3: ConcurrentHashMap<String, List<SampleData3>> = ConcurrentHashMap(),

) : Partitioning<SampleDataOnlyP3> {

    constructor() : this(

        partitioningOn = true
    )

    override fun failPartitionData(number: Int, error: Throwable) {

        Console.error(error)
    }

    override fun getClazz(): Class<SampleDataOnlyP3> {

        return SampleDataOnlyP3::class.java
    }

    override fun isPartitioningEnabled() = partitioningOn

    fun isPartitioningDisabled() = !partitioningOn

    override fun isPartitioningParallelized() = true

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

                    (data as ConcurrentHashMap<String, List<Any>>).forEach {

                        val key = it.key
                        val value = it.value

                        val children = ArrayList<SampleData3>()

                        value.forEach { item ->

                            if (item is LinkedTreeMap<*, *>) {

                                val instance = SampleData3(item as LinkedTreeMap<String, Any>)

                                children.add(instance)
                            }
                        }

                        partition3[key] = children
                    }


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

            0 -> object : TypeToken<ConcurrentHashMap<String, List<SampleData3>>>() {}.type

            else -> null
        }
    }
}
