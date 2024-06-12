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

data class PartitioningTestData @JsonCreator constructor(

    @JsonProperty("partitioningOn")
    @SerializedName("partitioningOn")
    private val partitioningOn: Boolean = true,

    @JsonProperty("partition1")
    @SerializedName("partition1")
    var partition1: CopyOnWriteArrayList<NestedData> = CopyOnWriteArrayList(),

    @JsonProperty("partition2")
    @SerializedName("partition2")
    var partition2: ConcurrentHashMap<UUID, NestedData> = ConcurrentHashMap(),

    @JsonProperty("partition3")
    @SerializedName("partition3")
    var partition3: ConcurrentHashMap<String, List<NestedDataSecondLevel>> = ConcurrentHashMap(),

    @JsonProperty("partition4")
    @SerializedName("partition4")
    var partition4: NestedDataSecondLevel? = null,

    @JsonProperty("partition5")
    @SerializedName("partition5")
    var partition5: String = "",

    @JsonProperty("partition6")
    @SerializedName("partition6")
    var partition6: CopyOnWriteArrayList<Long> = CopyOnWriteArrayList()

) : Partitional<PartitioningTestData> {

    constructor() : this(

        partitioningOn = true
    )

    override fun getClazz(): Class<PartitioningTestData> {

        return PartitioningTestData::class.java
    }

    override fun isPartitioningEnabled() = partitioningOn

    fun isPartitioningDisabled() = !partitioningOn

    override fun getPartitionCount() = 11

    override fun getPartitionData(number: Int): Any? {

        when (number) {

            0 -> return partition1
            1 -> return partition2
            2 -> return partition3
            3 -> return partition4
            4 -> return partition5
            5 -> return partition6

            else -> return null
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

                    partition1 = data as CopyOnWriteArrayList<NestedData>

                } catch (e: Exception) {

                    Timber.e(e)

                    return false
                }

                return true
            }

            1 -> {

                try {

                    partition2 = data as ConcurrentHashMap<UUID, NestedData>

                } catch (e: Exception) {

                    Timber.e(e)

                    return false
                }

                return true
            }

            2 -> {

                try {

                    partition3 = data as ConcurrentHashMap<String, List<NestedDataSecondLevel>>

                } catch (e: Exception) {

                    Timber.e(e)

                    return false
                }

                return true
            }

            3 -> {

                try {

                    partition4 = data as NestedDataSecondLevel

                } catch (e: Exception) {

                    Timber.e(e)

                    return false
                }

                return true
            }

            4 -> {

                try {

                    partition5 = data as String

                } catch (e: Exception) {

                    Timber.e(e)

                    return false
                }

                return true
            }

            5 -> {

                try {

                    partition6 = data as CopyOnWriteArrayList<Long>

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

        when (number) {

            0 -> return object : TypeToken<CopyOnWriteArrayList<NestedData>>() {}.type

            1 -> return object : TypeToken<ConcurrentHashMap<UUID, NestedData>>() {}.type

            2 -> return object : TypeToken<ConcurrentHashMap<String, List<NestedDataSecondLevel>>>() {}.type

            3 -> return object : TypeToken<NestedDataSecondLevel>() {}.type

            4 -> return object : TypeToken<String>() {}.type

            5 -> return object : TypeToken<CopyOnWriteArrayList<Long>>() {}.type

            else -> return null
        }
    }
}
