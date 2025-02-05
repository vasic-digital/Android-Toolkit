package com.redelf.commons.test.test_data.wrapper

import com.redelf.commons.logging.Console
import org.junit.Assert
import java.util.concurrent.ConcurrentHashMap

class StringToLongMapWrapper(map: ConcurrentHashMap<String, Long>) :

    TypeMapWrapper<String, Long>(map)
{

    constructor() : this(ConcurrentHashMap())

    override fun failPartitionData(number: Int, error: Throwable) {

        Console.error(error)
    }

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

                if (value is Number) {

                    this.data?.put(key.toString(), value.toLong())

                } else {

                    Assert.fail("Number was expected for the value: '$value'")
                }
            }

            Console.log("Data set: ${this.data}")

        } catch (e: Exception) {

            Console.error(e)

            return false
        }

        return true
    }
}