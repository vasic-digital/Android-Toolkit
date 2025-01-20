package com.redelf.commons.test.test_data.wrapper

import com.redelf.commons.logging.Console
import org.junit.Assert
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class UUIDtoStringMapWrapper(map: ConcurrentHashMap<UUID, String>) :

    TypeMapWrapper<UUID, String>(map)
{

    constructor() : this(ConcurrentHashMap())

    override fun failPartitionData(number: Int, error: Throwable) {

        Console.error(error)
    }

    override fun getClazz(): Class<UUIDtoStringMapWrapper> {

        return UUIDtoStringMapWrapper::class.java
    }

    override fun setPartitionData(number: Int, data: Any?): Boolean {

        if (number > 0) {

            Assert.fail("Unexpected partition number: $number")
        }

        try {

            this.data = ConcurrentHashMap<UUID, String>()

            (data as ConcurrentHashMap<*, *>).forEach { (key, value) ->

                this.data?.put(UUID.fromString(key.toString()), value as String)
            }

            Console.log("Data set: ${this.data}")

        } catch (e: Exception) {

            Console.error(e)

            return false
        }

        return true
    }
}