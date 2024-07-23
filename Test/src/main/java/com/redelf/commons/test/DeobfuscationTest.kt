package com.redelf.commons.test

import com.redelf.commons.extensions.deobfuscateString
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.security.obfuscation.DefaultObfuscator
import org.junit.Assert

abstract class DeobfuscationTest : BaseTest() {

    fun getDeobfuscatedData() : String {

        val resourceId = com.redelf.commons.R.string.ob_test

        val obfuscated = applicationContext.getString(resourceId)

        Assert.assertTrue(obfuscated.isNotEmpty())

        val deobfuscated = applicationContext.deobfuscateString(resourceId)

        Assert.assertTrue(deobfuscated.isNotEmpty())

        return deobfuscated
    }

    fun getSalt() : String {

        val deobfuscator = DefaultObfuscator.getStrategy()

        Assert.assertNotNull(deobfuscator)

        val salt =  deobfuscator.salt

        Assert.assertTrue(isNotEmpty(salt))

        return salt
    }

    fun testDeobfuscation(expectedSalt: String, expectedDeobfuscatedData: String) {

        val data = getDeobfuscatedData()
        val salt = getSalt()

        Assert.assertEquals(expectedSalt, salt)
        Assert.assertEquals(expectedDeobfuscatedData, data)
    }
}