package com.redelf.commons.test.test_data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import com.redelf.commons.logging.Console
import com.redelf.commons.partition.Partitioning
import java.lang.reflect.Type
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class SampleData @JsonCreator constructor(

    @JsonProperty("partitioningOn")
    @SerializedName("partitioningOn")
    private val partitioningOn: Boolean = true,

    @JsonProperty("partitioningParallelized")
    @SerializedName("partitioningParallelized")
    private var partitioningParallelized: Boolean = true,

    @JsonProperty("partition1")
    @SerializedName("partition1")
    var partition1: CopyOnWriteArrayList<SampleData2> = CopyOnWriteArrayList(),

    @JsonProperty("partition2")
    @SerializedName("partition2")
    var partition2: ConcurrentHashMap<UUID, SampleData2> = ConcurrentHashMap(),

    @JsonProperty("partition3")
    @SerializedName("partition3")
    var partition3: ConcurrentHashMap<String, List<SampleData3>> = ConcurrentHashMap(),

    @JsonProperty("partition4")
    @SerializedName("partition4")
    var partition4: SampleData3? = null,

    @JsonProperty("partition5")
    @SerializedName("partition5")
    var partition5: String = "",

    @JsonProperty("partition6")
    @SerializedName("partition6")
    var partition6: CopyOnWriteArrayList<Double> = CopyOnWriteArrayList()

) : Partitioning<SampleData> {

    constructor() : this(

        partitioningOn = true,
        partitioningParallelized = true
    )

    override fun failPartitionData(number: Int, error: Throwable) {

        Console.error(error)
    }

    override fun getClazz(): Class<SampleData> {

        return SampleData::class.java
    }

    override fun isPartitioningEnabled() = partitioningOn

    fun isPartitioningDisabled() = !partitioningOn

    override fun isPartitioningParallelized() = partitioningParallelized

    fun setPartitioningParallelized(async: Boolean) {

        partitioningParallelized = async
    }

    override fun getPartitionCount() = 6

    override fun getPartitionData(number: Int): Any? {

        return when (number) {

            0 -> partition1
            1 -> partition2
            2 -> partition3
            3 -> partition4
            4 -> partition5
            5 -> partition6

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

                    partition1 = CopyOnWriteArrayList()
                    partition1.addAll(data as CopyOnWriteArrayList<SampleData2>)

                } catch (e: Exception) {

                    Console.error(e)

                    return false
                }

                return true
            }

            1 -> {

                try {

                    partition2 = ConcurrentHashMap()
                    partition2.putAll(data as ConcurrentHashMap<UUID, SampleData2>)

                } catch (e: Exception) {

                    Console.error(e)

                    return false
                }

                return true
            }

            2 -> {

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

            3 -> {

                try {

                    partition4 = data as SampleData3

                } catch (e: Exception) {

                    Console.error(e)

                    return false
                }

                return true
            }

            4 -> {

                try {

                    partition5 = data as String

                } catch (e: Exception) {

                    Console.error(e)

                    return false
                }

                return true
            }

            5 -> {

                try {

                    partition6 = CopyOnWriteArrayList()
                    partition6.addAll(data as CopyOnWriteArrayList<Double>)

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

        when (number) {

            0 -> return object : TypeToken<CopyOnWriteArrayList<SampleData2>>() {}.type

            1 -> return object : TypeToken<ConcurrentHashMap<UUID, SampleData2>>() {}.type

            2 -> return object : TypeToken<ConcurrentHashMap<String, List<SampleData3>>>() {}.type

            3 -> return object : TypeToken<SampleData3>() {}.type

            4 -> return object : TypeToken<String>() {}.type

            5 -> return object : TypeToken<CopyOnWriteArrayList<Long>>() {}.type

            else -> return null
        }
    }
}
