package com.redelf.commons.test.test_data.wrapper

import com.google.gson.reflect.TypeToken
import com.redelf.commons.data.model.Wrapper
import com.redelf.commons.partition.Partitioning
import org.junit.Assert
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

abstract class TypeMapWrapper<K, T>(map: ConcurrentHashMap<K, T>) :

    Wrapper<ConcurrentHashMap<K, T>>(map),
    Partitioning<TypeMapWrapper<K, T>>
{

    constructor() : this(ConcurrentHashMap())

    override fun isPartitioningEnabled() = true

    override fun isPartitioningParallelized() = true

    override fun getPartitionCount() = 1

    override fun getPartitionData(number: Int): Any? {

        if (number > 0) {

            Assert.fail("Unexpected partition number: $number")
        }

        return takeData()
    }

    override fun getPartitionType(number: Int): Type? {

        if (number > 0) {

            Assert.fail("Unexpected partition number: $number")
        }

        return object : TypeToken<ConcurrentHashMap<K, T>>() {}.type
    }
}