package com.redelf.commons.test.compression

import com.redelf.commons.extensions.compress
import com.redelf.commons.extensions.compressAndEncrypt
import com.redelf.commons.extensions.decompress
import com.redelf.commons.extensions.decryptAndDecompress
import com.redelf.commons.extensions.recordException
import com.redelf.commons.test.BaseTest
import org.junit.Assert
import org.junit.Test
import javax.crypto.KeyGenerator

class LZ4StringCompressionTest : BaseTest() {

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

        /*
        * FIXME: Make sure that test works:
        *   java.lang.IllegalStateException: Default FirebaseApp is not initialized in this process com.redelf.commons.test. Make sure to call FirebaseApp.initializeApp(Context) first.
        */

        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256) // AES-256
        val secretKey = keyGen.generateKey()

        val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec euismod, nulla sit amet ultrices ultrices, ante massa tincidunt ante, eu tincidunt turpis ante eu ante. "

        val compressed = text.compressAndEncrypt(secretKey = secretKey)
        val decompressed = compressed.decryptAndDecompress(secretKey = secretKey)

        Assert.assertNotNull(compressed)
        Assert.assertNotEquals(text, compressed)
        Assert.assertEquals(text, decompressed)
        Assert.assertTrue(compressed.isNotEmpty() == true)

        val textLength = text.length
        val compressedLength = compressed.length

        Assert.assertTrue(compressedLength > 0)
        Assert.assertTrue(compressedLength < textLength)
    }
}