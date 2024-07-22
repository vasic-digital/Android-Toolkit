package com.redelf.commons.test

import org.junit.Assert
import org.junit.Test

class DeobfuscationTest : BaseTest() {

    @Test
    fun testDeobfuscation() {

        val obfuscated = applicationContext.getString(com.redelf.commons.R.string.ob_test)

        Assert.assertTrue(obfuscated.isNotEmpty())

        // TODO: Implement deobfuscation test
    }
}