package com.redelf.commons.test

import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.security.obfuscation.Obfuscator
import com.redelf.commons.security.obfuscation.ObfuscatorSalt
import com.redelf.commons.security.obfuscation.ObfuscatorSaltProvider
import org.junit.Assert
import org.junit.Test

class ObfuscatorTest : BaseTest() {

    @Test
    fun testObfuscation() {

        val saltProvider = object : ObfuscatorSaltProvider {

            override fun obtain() = ObfuscatorSalt(value = "t3sR_s@lt!")
        }

        val obfuscator = Obfuscator(saltProvider)

        listOf(

            "test",
            "TeSt",
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum vel enim at nisi commodo dignissim.",
            "1234567890....1234567890"

        ).forEach { input ->

            val obfuscated = obfuscator.obfuscate(input)

            Assert.assertTrue(isNotEmpty(obfuscated))
            Assert.assertNotEquals(input, obfuscated)
            Assert.assertTrue(input.length < obfuscated.length)

            val deobfuscated = obfuscator.deobfuscate(obfuscated)

            Assert.assertEquals(input, deobfuscated)
        }
    }
}