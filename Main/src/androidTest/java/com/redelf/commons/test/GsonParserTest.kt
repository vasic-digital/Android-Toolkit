package com.redelf.commons.test

import com.fasterxml.jackson.annotation.JsonCreator
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.GsonParser
import com.redelf.commons.persistance.serialization.CustomSerializable
import com.redelf.commons.persistance.serialization.DefaultCustomSerializer
import org.junit.Assert
import org.junit.Test

class GsonParserTest : BaseTest() {

    private val testString = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
            "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
            "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut " +
            "aliquip ex ea commod ..."

    private val testBytes = testString.toByteArray()

    private data class SimpleAsset @JsonCreator constructor(

        @SerializedName("bytes")
        var bytes: ByteArray? = null,
        @SerializedName("size")
        var size: Long? = 0L,
        @SerializedName("filename")
        var fileName: String? = "",
        @SerializedName("cid")
        var cid: String? = "",
        @SerializedName("mime")
        var mimeType: String? = ""

    ) {

        var name: String = fileName ?: ""
    }

    private data class CustomAsset @JsonCreator constructor(

        @SerializedName("bytes")
        var bytes: ByteArray? = null,
        @SerializedName("size")
        var size: Long? = 0L,
        @SerializedName("filename")
        var fileName: String? = "",
        @SerializedName("cid")
        var cid: String? = "",
        @SerializedName("mime")
        var mimeType: String? = ""

    ) : CustomSerializable {

        var name: String = fileName ?: ""

        override fun getCustomSerializations() = mapOf(

            "bytes" to DefaultCustomSerializer()
        )
    }

    @Test
    fun testGsonParser() {

        val simpleAsset = SimpleAsset(

            bytes = testBytes,
            size = System.currentTimeMillis(),
            fileName = testString,
            cid = testString,
            mimeType = testString
        )

        val customAsset = CustomAsset(

            bytes = testBytes,
            size = System.currentTimeMillis(),
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

        val parser = GsonParser(

            parserKey = "test.${System.currentTimeMillis()}",

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



    }
}