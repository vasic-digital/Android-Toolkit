package com.redelf.commons.test.test_data.wrapper

import com.google.gson.reflect.TypeToken
import com.redelf.commons.data.model.Wrapper
import com.redelf.commons.logging.Console
import com.redelf.commons.partition.Partitioning
import org.junit.Assert
import java.lang.reflect.Type
import java.util.concurrent.CopyOnWriteArrayList

abstract class TypeListWrapper<T>(list: CopyOnWriteArrayList<T>) :

    Wrapper<CopyOnWriteArrayList<T>>(list),
    Partitioning<TypeListWrapper<T>>

{

    constructor() : this(CopyOnWriteArrayList())

    override fun isPartitioningEnabled() = true

    override fun isPartitioningParallelized() = true

    override fun getPartitionCount() = 1

    override fun getPartitionData(number: Int): Any? {

        if (number > 0) {

            Assert.fail("Unexpected partition number: $number")
        }

        return takeData()
    }

    @Suppress("UNCHECKED_CAST")
    override fun setPartitionData(number: Int, data: Any?): Boolean {

        if (number > 0) {

            Assert.fail("Unexpected partition number: $number")
        }

        try {

            this.data = CopyOnWriteArrayList()
            this.data?.addAll(data as CopyOnWriteArrayList<T>)

        } catch (e: Exception) {

            Console.error(e)

            return false
        }

        return true
    }

    override fun getPartitionType(number: Int): Type? {

        if (number > 0) {

            Assert.fail("Unexpected partition number: $number")
        }

        return object : TypeToken<CopyOnWriteArrayList<T>>() {}.type
    }
}