package com.redelf.commons.test.test_data.wrapper

import com.redelf.commons.data.model.Wrapper
import com.redelf.commons.logging.Console
import com.redelf.commons.partition.Partitioning
import org.junit.Assert

abstract class TypeWrapper<T>(wrapped: T?) :

    Wrapper<T?>(wrapped),
    Partitioning<TypeWrapper<T?>>

{

    constructor() : this(null)

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

            this.data = data as T?

        } catch (e: Exception) {

            Console.error(e)

            return false
        }

        return true
    }
}