package com.redelf.commons.test

import com.google.gson.GsonBuilder
import com.redelf.commons.extensions.GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.suspendable.Obtain
import com.redelf.commons.persistance.GsonParser
import com.redelf.commons.test.test_data.CustomAsset
import com.redelf.commons.test.test_data.ExtendedCustomAsset
import com.redelf.commons.test.test_data.SimpleAsset
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class GsonParserTest : BaseTest() {

    private val testString = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
            "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
            "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut " +
            "aliquip ex ea commod ..."

    private val testBytes = testString.toByteArray()

    @Before
    fun prepare() {

        Console.initialize(failOnError = true)

        Console.log("Console initialized: $this")

        GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK.set(true)
    }

    @Test
    fun testGsonParser() {

        val timestamp = System.currentTimeMillis()

        val simpleAsset = SimpleAsset(

            bytes = testBytes,
            size = timestamp,
            fileName = testString,
            cid = testString,
            mimeType = testString
        )

        val customAsset = CustomAsset(

            bytes = testBytes,
            size = timestamp,
            fileName = testString,
            cid = testString,
            mimeType = testString
        )

        Assert.assertEquals(simpleAsset.cid, customAsset.cid)
        Assert.assertEquals(simpleAsset.size, customAsset.size)
        Assert.assertEquals(simpleAsset.bytes, customAsset.bytes)
        Assert.assertEquals(simpleAsset.fileName, customAsset.fileName)
        Assert.assertEquals(simpleAsset.mimeType, customAsset.mimeType)

        val gsonBuilder = GsonBuilder()
            .enableComplexMapKeySerialization()

        val parser = GsonParser.instantiate(

            "test.$timestamp",
            null,
            true,

            object : Obtain<GsonBuilder> {

                override fun obtain(): GsonBuilder {

                    return gsonBuilder
                }
            }
        )

        val simpleJson = parser.toJson(simpleAsset)

        Assert.assertNotNull(simpleJson)
        Assert.assertTrue(isNotEmpty(simpleJson))

        val customJson = parser.toJson(customAsset)

        Assert.assertNotNull(customJson)
        Assert.assertTrue(isNotEmpty(customJson))

        val simpleJson2 = parser.toJson(simpleAsset)
        val customJson2 = parser.toJson(customAsset)

        Assert.assertEquals(simpleJson, simpleJson2)
        Assert.assertEquals(customJson, customJson2)

        val simpleDeserialized = parser.fromJson<SimpleAsset?>(simpleJson, SimpleAsset::class.java)

        Assert.assertNotNull(simpleDeserialized)

        val customDeserialized = parser.fromJson<CustomAsset?>(customJson, CustomAsset::class.java)

        Assert.assertNotNull(customDeserialized)

        Assert.assertEquals(simpleDeserialized?.cid, simpleAsset.cid)
        Assert.assertEquals(simpleDeserialized?.size, simpleAsset.size)
        Assert.assertEquals(simpleDeserialized?.mimeType, simpleAsset.mimeType)
        Assert.assertEquals(simpleDeserialized?.fileName, simpleAsset.fileName)
        Assert.assertEquals(simpleDeserialized?.bytes?.size, simpleAsset.bytes?.size)

        Assert.assertNotNull(simpleDeserialized?.bytes)
        Assert.assertTrue((simpleDeserialized?.bytes?.size ?: 0) > 0)

        Assert.assertEquals(customDeserialized?.cid?.length ?: 0, customAsset.cid?.length ?: -1)
        Assert.assertEquals(customDeserialized?.cid, customAsset.cid)

        Assert.assertEquals(customDeserialized?.size, customAsset.size)
        Assert.assertEquals(customDeserialized?.fileName, customAsset.fileName)
        Assert.assertEquals(customDeserialized?.mimeType, customAsset.mimeType)

        Assert.assertNotNull(customDeserialized?.bytes)
        Assert.assertTrue((customDeserialized?.bytes?.size ?: 0) > 0)
        Assert.assertEquals(customDeserialized?.bytes?.size, customAsset.bytes?.size)
    }

    @Test
    fun testNestedObjects() {

        val timestamp = System.currentTimeMillis()

        val customAsset = CustomAsset(

            bytes = testBytes,
            size = timestamp,
            fileName = testString,
            cid = testString,
            mimeType = testString
        )

        val customAsset2 = CustomAsset(

            bytes = "test".toByteArray(),
            size = timestamp,
            fileName = "$testString.2",
            cid = "$testString.2",
            mimeType = "$testString.2",
        )

        val extended = ExtendedCustomAsset(

            bytes = testBytes,
            size = timestamp,
            fileName = testString,
            cid = testString,
            mimeType = testString,
            customAsset = customAsset,
            customAssets = listOf(customAsset, customAsset2)
        )

        val gsonBuilder = GsonBuilder()
            .enableComplexMapKeySerialization()

        val parser = GsonParser.instantiate(

            "test.$timestamp",
            null,
            true,

            object : Obtain<GsonBuilder> {

                override fun obtain(): GsonBuilder {

                    return gsonBuilder
                }
            }
        )

        val customJson = parser.toJson(extended)

        Assert.assertNotNull(customJson)
        Assert.assertTrue(isNotEmpty(customJson))

        val customDeserialized = parser
            .fromJson<ExtendedCustomAsset?>(customJson, ExtendedCustomAsset::class.java)

        Assert.assertNotNull(customDeserialized)

        Assert.assertEquals(customDeserialized?.cid?.length ?: 0, extended.cid?.length ?: -1)
        Assert.assertEquals(customDeserialized?.cid, extended.cid)

        Assert.assertEquals(customDeserialized?.size, extended.size)
        Assert.assertEquals(customDeserialized?.fileName, extended.fileName)
        Assert.assertEquals(customDeserialized?.mimeType, extended.mimeType)

        Assert.assertNotNull(customDeserialized?.bytes)
        Assert.assertTrue((customDeserialized?.bytes?.size ?: 0) > 0)
        Assert.assertEquals(customDeserialized?.bytes?.size, extended.bytes?.size)
    }

    @Test
    fun testPrimitives() {

        val timestamp = System.currentTimeMillis()

        val gsonBuilder = GsonBuilder()
            .enableComplexMapKeySerialization()

        val parser = GsonParser.instantiate(

            "test.$timestamp",
            null,
            true,

            object : Obtain<GsonBuilder> {

                override fun obtain(): GsonBuilder {

                    return gsonBuilder
                }
            }
        )

        mapOf(

            0 to Int::class.java,
            1 to Int::class.java,
            timestamp to Long::class.java,
            timestamp.toInt() to Int::class.java,
            timestamp.toFloat() to Float::class.java,
            timestamp.toDouble() to Double::class.java,
            timestamp.toString() to String()::class.java,
            true to Boolean::class.java,
            false to Boolean::class.java

        ).forEach { key, value ->

            val deserialized = parser.fromJson<Any>(key.toString(), value)

            Assert.assertNotNull(deserialized)
            Assert.assertEquals(key, deserialized)
        }
    }
}