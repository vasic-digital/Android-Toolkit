package com.redelf.commons.test

import com.redelf.commons.extensions.deobfuscateString
import org.junit.Assert
import org.junit.Test

class DeobfuscationTest : BaseTest() {

    @Test
    fun testDeobfuscation() {

        val resourceId = com.redelf.commons.R.string.ob_test

        val obfuscated = applicationContext.getString(resourceId)

        Assert.assertTrue(obfuscated.isNotEmpty())

        val deobfuscated = applicationContext.deobfuscateString(resourceId)

        Assert.assertTrue(deobfuscated.isNotEmpty())

        // TODO: Complete
    }
}