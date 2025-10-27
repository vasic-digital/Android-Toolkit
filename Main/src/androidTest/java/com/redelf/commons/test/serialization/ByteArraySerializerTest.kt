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


package com.redelf.commons.test.serialization

import com.google.gson.GsonBuilder
import com.redelf.commons.persistance.serialization.ByteArraySerializer
import com.redelf.commons.persistance.serialization.ByteArrayTypeAdapter
import com.redelf.commons.test.BaseTest
import org.junit.Assert
import org.junit.Test
import java.nio.charset.Charset

class ByteArraySerializerTest : BaseTest() {

    // TODO: We need to incorporate the test which will verify if custom serializer has been
    //  invoked at all and applied

    private val testString = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
            "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
            "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut " +
            "aliquip ex ea commod ..."

    private val testBytes = testString.toByteArray()

    private class BytesWrapper(val bytes: ByteArray?)

    @Test
    fun testByteArraySerializer() {

        val testByteArraySerializer = ByteArraySerializer(

            applicationContext, "test.${System.currentTimeMillis()}", true
        )

        val serialized = testByteArraySerializer.serialize("test", testBytes)

        Assert.assertTrue(serialized)

        val deserialized = testByteArraySerializer.deserialize("test")

        Assert.assertNotNull(deserialized)

        deserialized?.let {

            Assert.assertEquals(testString, String(it, Charset.forName("UTF-8")))
        }
    }

    @Test
    fun testByteArrayTypeAdapter() {

        val gson = GsonBuilder()
            .registerTypeAdapter(

                ByteArray::class.java,
                ByteArrayTypeAdapter(applicationContext, "test")
            )
            .create()

        val wrapper = BytesWrapper(testBytes)
        val json = gson.toJson(wrapper)

        Assert.assertNotNull(json)

        val wrapper2 = gson.fromJson(json, BytesWrapper::class.java)

        Assert.assertNotNull(json)

        Assert.assertNotNull(wrapper.bytes)
        Assert.assertNotNull(wrapper2.bytes)

        var testOk = false

        wrapper.bytes?.let { wBytes ->
            wrapper2.bytes?.let { wBytes2 ->

                val wString = String(wBytes, Charset.forName("UTF-8"))
                val wString2 = String(wBytes2, Charset.forName("UTF-8"))

                Assert.assertEquals(wString, wString2)
                Assert.assertEquals(wString, testString)
                Assert.assertEquals(wString2, testString)

                testOk = true
            }
        }

        Assert.assertTrue(testOk)
    }

    @Test
    fun testByteArraySerializerWithNoEncryption() {

        val testByteArraySerializer = ByteArraySerializer(

            applicationContext,
            "test.${System.currentTimeMillis()}",
            false
        )

        val serialized = testByteArraySerializer.serialize("test", testBytes)

        Assert.assertTrue(serialized)

        val deserialized = testByteArraySerializer.deserialize("test")

        Assert.assertNotNull(deserialized)

        deserialized?.let {

            Assert.assertEquals(testString, String(it, Charset.forName("UTF-8")))
        }
    }

    @Test
    fun testByteArrayTypeAdapterWithNoEncryption() {

        val gson = GsonBuilder()
            .registerTypeAdapter(

                ByteArray::class.java,
                ByteArrayTypeAdapter(applicationContext, "test", false)
            )
            .create()

        val wrapper = BytesWrapper(testBytes)
        val json = gson.toJson(wrapper)

        Assert.assertNotNull(json)

        val wrapper2 = gson.fromJson(json, BytesWrapper::class.java)

        Assert.assertNotNull(json)

        Assert.assertNotNull(wrapper.bytes)
        Assert.assertNotNull(wrapper2.bytes)

        var testOk = false

        wrapper.bytes?.let { wBytes ->
            wrapper2.bytes?.let { wBytes2 ->

                val wString = String(wBytes, Charset.forName("UTF-8"))
                val wString2 = String(wBytes2, Charset.forName("UTF-8"))

                Assert.assertEquals(wString, wString2)
                Assert.assertEquals(wString, testString)
                Assert.assertEquals(wString2, testString)

                testOk = true
            }
        }

        Assert.assertTrue(testOk)
    }
}