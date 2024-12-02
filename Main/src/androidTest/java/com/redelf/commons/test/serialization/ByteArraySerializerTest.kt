package com.redelf.commons.test.serialization

import com.redelf.commons.persistance.ByteArraySerializer
import com.redelf.commons.test.BaseTest
import org.junit.Assert
import org.junit.Test
import java.nio.charset.Charset

class ByteArraySerializerTest : BaseTest() {

    @Test
    fun testByteArraySerializer() {

        val testString = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commod"
        val testBytes = testString.toByteArray()

        val testByteArraySerializer = ByteArraySerializer(

            applicationContext, "test.${System.currentTimeMillis()}"
        )

        val serialized = testByteArraySerializer.serialize("test", testBytes)

        Assert.assertTrue(serialized)

        val deserialized = testByteArraySerializer.deserialize("test")

        Assert.assertNotNull(deserialized)

        deserialized?.let {

            Assert.assertEquals(testString, String(it, Charset.forName("UTF-8")))
        }
    }
}