package com.redelf.commons.test.compression

import com.redelf.commons.extensions.compress
import com.redelf.commons.extensions.decompress
import com.redelf.commons.test.BaseTest
import org.junit.Assert
import org.junit.Test

class LZ4StringCompressionTest : BaseTest() {

    @Test
    fun testLZ4() {

        val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec euismod, nulla sit amet ultrices ultrices, ante massa tincidunt ante, eu tincidunt turpis ante eu ante. "

        val compressed = text.compress(lz4 = true)
        val decompressed = compressed?.decompress(lz4 = true)

        Assert.assertNotNull(compressed)
        Assert.assertNotEquals(text, compressed)
        Assert.assertEquals(text, decompressed)
        Assert.assertTrue(compressed?.length != 0)

        val textLength = text.length
        val compressedLength = compressed?.length ?: 0

        Assert.assertTrue(compressedLength > 0)
        Assert.assertTrue(compressedLength < textLength)
    }
}