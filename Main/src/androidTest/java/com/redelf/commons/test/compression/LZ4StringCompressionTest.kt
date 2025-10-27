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


package com.redelf.commons.test.compression

import com.redelf.commons.extensions.GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK
import com.redelf.commons.extensions.compress
import com.redelf.commons.extensions.compressAndEncrypt
import com.redelf.commons.extensions.decompress
import com.redelf.commons.extensions.decryptAndDecompress
import com.redelf.commons.logging.Console
import com.redelf.commons.test.BaseTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class LZ4StringCompressionTest : BaseTest() {

    @Before
    fun prepare() {

        Console.initialize(failOnError = true)

        Console.log("Console initialized: $this")

        GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK.set(true)
    }

    @Test
    fun testLZ4() {

        val text =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec euismod, nulla sit amet ultrices ultrices, ante massa tincidunt ante, eu tincidunt turpis ante eu ante. "

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

        val tag = "Test :: LZ4WithEncryption ::"

        var text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec euismod, " +
                "nulla sit amet ultrices ultrices, ante massa tincidunt ante, eu " +
                "tincidunt turpis ante eu ante."

        Console.log("$tag START")

        (0..10).forEach {

            text += text

            val tag = "$tag Iteration = ${it + 1} ::"

            var start = System.currentTimeMillis()

            val compressed = text.compressAndEncrypt()

            Console.log(

                "$tag COMPRESSED :: Time = ${System.currentTimeMillis() - start} ms, " +
                        "Size = ${compressed.toByteArray().size} bytes, " +
                        "Original Size = ${text.toByteArray().size} bytes"
            )

            Assert.assertNotNull(compressed)
            Assert.assertNotEquals(text, compressed)

            start = System.currentTimeMillis()

            val decompressed = compressed.decryptAndDecompress()

            Console.log(

                "$tag DECOMPRESSED :: Time = ${System.currentTimeMillis() - start} ms, " +
                        "Size = ${decompressed.toByteArray().size} bytes, " +
                        "Original Size = ${text.toByteArray().size} bytes"
            )

            Assert.assertEquals(text, decompressed)
            Assert.assertTrue(compressed.isNotEmpty())
        }

        Console.log("$tag END")
    }
}