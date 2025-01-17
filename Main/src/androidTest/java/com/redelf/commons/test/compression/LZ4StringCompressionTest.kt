package com.redelf.commons.test.compression

import com.redelf.commons.extensions.GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK
import com.redelf.commons.extensions.compress
import com.redelf.commons.extensions.compressAndEncrypt
import com.redelf.commons.extensions.decompress
import com.redelf.commons.extensions.decryptAndDecompress
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.test.BaseTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import javax.crypto.KeyGenerator

class LZ4StringCompressionTest : BaseTest() {

    @Before
    fun prepare() {

        Console.initialize(failOnError = true)

        Console.log("Console initialized: $this")

        GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK.set(true)
    }

    @Test
    fun testLZ4() {

        val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec euismod, nulla sit amet ultrices ultrices, ante massa tincidunt ante, eu tincidunt turpis ante eu ante. "

        val compressed = text.compress(lz4 = true)
        val decompressed = compressed?.decompress(lz4 = true)

        Assert.assertNotNull(compressed)
        Assert.assertNotEquals(text, compressed)
        Assert.assertEquals(text, decompressed)
        Assert.assertTrue(compressed?.isNotEmpty() == true)

        val textLength = text.length
        val compressedLength = compressed?.size ?: 0

        Assert.assertTrue(compressedLength > 0)
        Assert.assertTrue(compressedLength < textLength)
    }

    @Test
    fun testLZ4WithEncryption() {

        val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit."

        /*
        * FIXME: Make sure that test works:
        *   net.jpountz.lz4.LZ4Exception: Malformed input at 127
        */
        val compressed = text.compressAndEncrypt()

        Assert.assertNotNull(compressed)
        Assert.assertNotEquals(text, compressed)

        val decompressed = compressed.decryptAndDecompress()

        Assert.assertEquals(text, decompressed)
        Assert.assertTrue(compressed.isNotEmpty() == true)

        val textLength = text.length
        val compressedLength = compressed.length

        Assert.assertTrue(compressedLength > 0)
        Assert.assertTrue(compressedLength < textLength)
    }
}