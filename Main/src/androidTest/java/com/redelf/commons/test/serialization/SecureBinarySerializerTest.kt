package com.redelf.commons.test.serialization

import com.google.gson.GsonBuilder
import com.redelf.commons.persistance.serialization.SecureBinarySerializer
import com.redelf.commons.persistance.serialization.ByteArrayTypeAdapter
import com.redelf.commons.test.BaseTest
import org.junit.Assert
import org.junit.Test
import java.nio.charset.Charset

class SecureBinarySerializerTest : BaseTest() {

    // TODO: We need to incorporate the test which will verify if custom serializer has been
    //  invoked at all and applied

    private val testString = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
            "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
            "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut " +
            "aliquip ex ea commod ..."

    private val testBytes = testString.toByteArray()

    private class BytesWrapper(val bytes: ByteArray?)

    @Test
    fun testSecureBinarySerializer() {

        val testSecureBinarySerializer = SecureBinarySerializer(

            applicationContext, "test.${System.currentTimeMillis()}", true
        )

        val serialized = testSecureBinarySerializer.serialize("test", testBytes)

        Assert.assertTrue(serialized)

        val deserialized = testSecureBinarySerializer.deserialize("test")

        Assert.assertNotNull(deserialized)

        deserialized?.let {

            Assert.assertEquals(testString, String(it as ByteArray, Charset.forName("UTF-8")))
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
    fun testSecureBinarySerializerWithNoEncryption() {

        val testSecureBinarySerializer = SecureBinarySerializer(

            applicationContext,
            "test.${System.currentTimeMillis()}",
            false
        )

        val serialized = testSecureBinarySerializer.serialize("test", testBytes)

        Assert.assertTrue(serialized)

        val deserialized = testSecureBinarySerializer.deserialize("test")

        Assert.assertNotNull(deserialized)

        deserialized?.let {

            Assert.assertEquals(testString, String(it as ByteArray, Charset.forName("UTF-8")))
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

    @Test
    fun testSecureBinarySerializerLargeData() {
        // Test with large data to verify streaming functionality
        val largeData = ByteArray(1024 * 1024) { (it % 256).toByte() } // 1MB of data

        val testSecureBinarySerializer = SecureBinarySerializer(
            applicationContext,
            "test.large.${System.currentTimeMillis()}",
            true
        )

        val serialized = testSecureBinarySerializer.serialize("testLarge", largeData)
        Assert.assertTrue(serialized)

        val deserialized = testSecureBinarySerializer.deserialize("testLarge")
        Assert.assertNotNull(deserialized)

        deserialized?.let { data ->
            val byteArray = data as ByteArray
            Assert.assertEquals(largeData.size, byteArray.size)
            Assert.assertArrayEquals(largeData, byteArray)
        }
    }

    @Test
    fun testSecureBinarySerializerSizeValidation() {
        // Test size validation limits
        val testSecureBinarySerializer = SecureBinarySerializer(
            applicationContext,
            "test.validation.${System.currentTimeMillis()}",
            false
        )

        // Create data that's too large (over 100MB limit)
        val oversizedData = ByteArray(101 * 1024 * 1024) { 0 } // 101MB

        // This should fail due to size validation
        val serialized = testSecureBinarySerializer.serialize("testOversize", oversizedData)
        Assert.assertFalse(serialized)
    }
}