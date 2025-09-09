package com.redelf.commons.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.redelf.commons.extensions.GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.StreamingJsonParser
import com.redelf.commons.test.test_data.CustomAsset
import com.redelf.commons.test.test_data.ExtendedCustomAsset
import com.redelf.commons.test.test_data.SimpleAsset
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class StreamingJsonParserTest : BaseTest() {

    private val testString = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
            "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
            "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut " +
            "aliquip ex ea commodo consequat."

    private val testBytes = testString.toByteArray()

    @Before
    fun prepare() {
        Console.initialize(failOnError = true)
        Console.log("Console initialized: $this")
        GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK.set(true)
    }

    @Test
    fun testStreamingJsonParser() {
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

        val parser = StreamingJsonParser.instantiate(
            "test.$timestamp",
            null,
            true,
            object : Obtain<ObjectMapper> {
                override fun obtain(): ObjectMapper {
                    return ObjectMapper().apply {
                        configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                    }
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
            mimeType = "$testString.2"
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

        val parser = StreamingJsonParser.instantiate(
            "test.$timestamp",
            null,
            true,
            object : Obtain<ObjectMapper> {
                override fun obtain(): ObjectMapper {
                    return ObjectMapper().apply {
                        configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                    }
                }
            }
        )

        val customJson = parser.toJson(extended)

        Assert.assertNotNull(customJson)
        Assert.assertTrue(isNotEmpty(customJson))

        val customDeserialized = parser.fromJson<ExtendedCustomAsset?>(customJson, ExtendedCustomAsset::class.java)

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

        val parser = StreamingJsonParser.instantiate(
            "test.$timestamp",
            null,
            true,
            object : Obtain<ObjectMapper> {
                override fun obtain(): ObjectMapper {
                    return ObjectMapper().apply {
                        configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                    }
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

    @Test
    fun testLargeObjectSerialization() {
        val timestamp = System.currentTimeMillis()
        
        val parser = StreamingJsonParser.instantiate(
            "large-test.$timestamp",
            null,
            false
        )
        
        // Create a large object with nested structures
        val largeObject = createLargeNestedObject(1000) // 1000 levels deep
        
        val startTime = System.currentTimeMillis()
        val json = parser.toJson(largeObject)
        val serializationTime = System.currentTimeMillis() - startTime
        
        Assert.assertNotNull(json)
        Assert.assertTrue(isNotEmpty(json))
        
        Console.log("Large object serialization took: ${serializationTime}ms")
        
        val deserializationStartTime = System.currentTimeMillis()
        val deserialized = parser.fromJson<Map<String, Any>>(json, Map::class.java)
        val deserializationTime = System.currentTimeMillis() - deserializationStartTime
        
        Assert.assertNotNull(deserialized)
        Console.log("Large object deserialization took: ${deserializationTime}ms")
        
        // Verify performance metrics
        val metrics = StreamingJsonParser.getPerformanceMetrics()
        Assert.assertTrue("Should have processed operations", metrics["totalOperations"]!! > 0)
        
        Console.log("Performance metrics: $metrics")
    }
    
    private fun createLargeNestedObject(depth: Int): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        
        repeat(10) { i ->
            map["key_$i"] = "value_$i"
            map["number_$i"] = i
            map["boolean_$i"] = i % 2 == 0
        }
        
        if (depth > 0) {
            map["nested"] = createLargeNestedObject(depth - 1)
        }
        
        return map
    }
}