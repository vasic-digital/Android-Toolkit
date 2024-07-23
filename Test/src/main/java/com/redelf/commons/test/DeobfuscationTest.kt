package com.redelf.commons.test

import com.redelf.commons.extensions.deobfuscateString
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.yieldWhile
import com.redelf.commons.security.obfuscation.DefaultObfuscator
import com.redelf.commons.security.obfuscation.ObfuscatorSalt
import org.junit.Assert

abstract class DeobfuscationTest : BaseTest() {

    private fun getDeobfuscatedData() : String {

        val resourceId = com.redelf.commons.R.string.ob_test

        val obfuscated = applicationContext.getString(resourceId)

        Assert.assertTrue(obfuscated.isNotEmpty())

        val deobfuscated = applicationContext.deobfuscateString(resourceId)

        Assert.assertTrue(deobfuscated.isNotEmpty())

        return deobfuscated
    }

    private fun getSalt() : ObfuscatorSalt {

        val deobfuscator = DefaultObfuscator.getStrategy()

        Assert.assertNotNull(deobfuscator)

        val salt =  deobfuscator.saltProvider.obtain()

        Assert.assertNotNull(salt)
        Assert.assertNull(salt.error)
        Assert.assertTrue(isNotEmpty(salt.value))

        return salt
    }

    private fun waitForObfuscator(timeoutInMilliseconds: Long = 5000L) {

        yieldWhile(

            timeoutInMilliseconds = timeoutInMilliseconds

        ) {

            DefaultObfuscator.isNotReady()
        }

        Assert.assertTrue(DefaultObfuscator.isReady())
    }

    fun testDeobfuscation(

        expectedSalt: String,
        expectedDeobfuscatedData: String,
        timeoutInMilliseconds: Long = 5000L

    ) {

        waitForObfuscator(timeoutInMilliseconds)

        var salt = getSalt()

        Assert.assertTrue(salt.isFirstTimeObtained)

        salt = getSalt()

        Assert.assertTrue(salt.isFromCache())

        val data = getDeobfuscatedData()

        Assert.assertEquals(expectedSalt, salt.value)
        Assert.assertEquals(expectedDeobfuscatedData, data)
    }
}