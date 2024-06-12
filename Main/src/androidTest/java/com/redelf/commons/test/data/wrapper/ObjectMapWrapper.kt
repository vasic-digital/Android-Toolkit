package com.redelf.commons.test.data.wrapper

import com.redelf.commons.logging.Timber
import com.redelf.commons.test.data.SampleData3
import org.junit.Assert
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ObjectMapWrapper(map: ConcurrentHashMap<UUID, SampleData3>) :

    TypeMapWrapper<UUID, SampleData3>(map)
{

    constructor() : this(ConcurrentHashMap())

    override fun getClazz(): Class<ObjectMapWrapper> {

        return ObjectMapWrapper::class.java
    }

    @Suppress("UNCHECKED_CAST")
    override fun setPartitionData(number: Int, data: Any?): Boolean {

        if (number > 0) {

            Assert.fail("Unexpected partition number: $number")
        }

        try {

            this.data = ConcurrentHashMap<UUID, SampleData3>()

            (data as ConcurrentHashMap<*, *>).forEach { (key, value) ->

                this.data?.put(UUID.fromString(key.toString()), value as SampleData3)
            }

            Timber.v("Data set: ${this.data}")

        } catch (e: Exception) {

            Timber.e(e)

            return false
        }

        return true
    }
}