package com.redelf.commons.test.data.wrapper

import com.google.gson.reflect.TypeToken
import com.redelf.commons.logging.Timber
import com.redelf.commons.model.Wrapper
import com.redelf.commons.partition.Partitional
import org.junit.Assert
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

abstract class TypeMapWrapper<K, T>(map: ConcurrentHashMap<K, T>) :

    Wrapper<ConcurrentHashMap<K, T>>(map),
    Partitional<TypeMapWrapper<K, T>>
{

    constructor() : this(ConcurrentHashMap())

    override fun isPartitioningEnabled() = true

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