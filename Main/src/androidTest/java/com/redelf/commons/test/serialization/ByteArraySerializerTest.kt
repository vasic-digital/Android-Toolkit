package com.redelf.commons.test.serialization

import com.google.gson.GsonBuilder
import com.redelf.commons.persistance.serialization.ByteArraySerializer
import com.redelf.commons.persistance.serialization.ByteArrayTypeAdapter
import com.redelf.commons.test.BaseTest
import org.junit.Assert
import org.junit.Test
import java.nio.charset.Charset

class ByteArraySerializerTest : BaseTest() {

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