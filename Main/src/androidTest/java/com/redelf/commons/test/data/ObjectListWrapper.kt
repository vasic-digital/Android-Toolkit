package com.redelf.commons.test.data

import com.google.gson.reflect.TypeToken
import com.redelf.commons.logging.Timber
import com.redelf.commons.model.Wrapper
import com.redelf.commons.partition.Partitional
import org.junit.Assert
import java.lang.reflect.Type
import java.util.concurrent.CopyOnWriteArrayList

class ObjectListWrapper(list: CopyOnWriteArrayList<SampleData3>) :

    Wrapper<CopyOnWriteArrayList<SampleData3>>(list),
    Partitional<ObjectListWrapper>

{

    constructor() : this(CopyOnWriteArrayList())

    override fun isPartitioningEnabled() = true

    override fun getPartitionCount() = 1

    override fun getPartitionData(number: Int): Any {

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

            this.data = data as CopyOnWriteArrayList<SampleData3>

        } catch (e: Exception) {

            Timber.e(e)

            return false
        }

        return true
    }

    override fun getPartitionType(number: Int): Type? {

        if (number > 0) {

            Assert.fail("Unexpected partition number: $number")
        }

        return object : TypeToken<CopyOnWriteArrayList<SampleData3>>() {}.type
    }

    override fun getClazz(): Class<ObjectListWrapper> {

        return ObjectListWrapper::class.java
    }
}