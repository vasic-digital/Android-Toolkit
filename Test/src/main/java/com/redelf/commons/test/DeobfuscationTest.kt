package com.redelf.commons.test

import com.redelf.commons.extensions.deobfuscateString
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
}