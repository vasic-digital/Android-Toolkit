package com.redelf.commons.test.data.wrapper

import com.redelf.commons.logging.Timber
import org.junit.Assert
import java.util.concurrent.ConcurrentHashMap

class StringToLongMapWrapper(map: ConcurrentHashMap<String, Long>) :

    TypeMapWrapper<String, Long>(map)
{

    constructor() : this(ConcurrentHashMap())

    override fun getClazz(): Class<StringToLongMapWrapper> {

        return StringToLongMapWrapper::class.java
    }

    override fun setPartitionData(number: Int, data: Any?): Boolean {

        if (number > 0) {

            Assert.fail("Unexpected partition number: $number")
        }

        try {

            this.data = ConcurrentHashMap<String, Long>()

            (data as ConcurrentHashMap<*, *>).forEach { (key, value) ->

                this.data?.put(key.toString(), value as Long)
            }

            Timber.v("Data set: ${this.data}")

        } catch (e: Exception) {

            Timber.e(e)

            return false
        }

        return true
    }
}