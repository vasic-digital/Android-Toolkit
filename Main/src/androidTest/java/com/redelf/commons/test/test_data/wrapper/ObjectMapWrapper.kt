/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.redelf.commons.test.test_data.wrapper

import com.redelf.commons.logging.Console
import com.redelf.commons.test.test_data.SampleData3
import org.junit.Assert
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ObjectMapWrapper(map: ConcurrentHashMap<UUID, SampleData3>) :

    TypeMapWrapper<UUID, SampleData3>(map)
{

    constructor() : this(ConcurrentHashMap())

    override fun failPartitionData(number: Int, error: Throwable) {

        Console.error(error)
    }

    override fun getClazz(): Class<ObjectMapWrapper> {

        return ObjectMapWrapper::class.java
    }

    override fun setPartitionData(number: Int, data: Any?): Boolean {

        if (number > 0) {

            Assert.fail("Unexpected partition number: $number")
        }

        try {

            this.data = ConcurrentHashMap<UUID, SampleData3>()

            (data as ConcurrentHashMap<*, *>).forEach { (key, value) ->

                this.data?.put(UUID.fromString(key.toString()), value as SampleData3)
            }

            Console.log("Data set: ${this.data}")

        } catch (e: Throwable) {

            Console.error(e)

            return false
        }

        return true
    }
}