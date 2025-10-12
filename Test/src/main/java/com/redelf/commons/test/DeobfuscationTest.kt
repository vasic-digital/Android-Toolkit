package com.redelf.commons.test

abstract class DeobfuscationTest : BaseTest() {

    // FIXME: Fix the test

//    protected fun getObfuscatedString(): String {
//
//        val input = "test"
//        val obfuscated = input.obfuscate()
//        val deobfuscated = obfuscated.deobfuscate()
//
//        Assert.assertEquals(input, deobfuscated)
//        Assert.assertNotEquals(input, obfuscated)
//
//        return obfuscated
//    }
//
//    private fun getObfuscatedData(): String {
//
//        val resourceId = com.redelf.commons.R.string.ob_test
//
//        val obfuscated = applicationContext.getString(resourceId)
//
//        Assert.assertTrue(obfuscated.isNotEmpty())
//
//        return obfuscated
//    }
//
//    private fun getDeobfuscatedData(): String {
//
//        val resourceId = com.redelf.commons.R.string.ob_test
//
//        val obfuscated = applicationContext.getString(resourceId)
//
//        Assert.assertTrue(obfuscated.isNotEmpty())
//
//        val deobfuscated = applicationContext.deobfuscateString(resourceId)
//
//        Assert.assertTrue(deobfuscated.isNotEmpty())
//
//        return deobfuscated
//    }
//
//    private fun getSalt(): ObfuscatorSalt? {
//
//        val deobfuscator = DefaultObfuscator.getStrategy()
//
//        Assert.assertNotNull(deobfuscator)
//
//        val salt = deobfuscator.saltProvider.obtain()
//
//        Assert.assertNotNull(salt)
//        Assert.assertNull(salt?.error)
//        Assert.assertTrue(isNotEmpty(salt?.takeValue()))
//
//        return salt
//    }
//
//    private fun waitForObfuscator(timeoutInMilliseconds: Long = 5000L) {
//
//        yieldWhile(
//
//            timeoutInMilliseconds = timeoutInMilliseconds
//
//        ) {
//
//            DefaultObfuscator.isNotReady()
//        }
//
//        Assert.assertTrue(DefaultObfuscator.isReady())
//    }
//
//    @Before
//    fun cleanup() {
//
//        val cleared = SecretsManager.obtain().reset()
//
//        Assert.assertTrue(cleared)
//    }
//
//    fun testDeobfuscation(
//
//        expectedSalt: String,
//        expectedDeobfuscatedData: String,
//        expectedObfuscatorIdentity: String,
//        timeoutInMilliseconds: Long = 5000L
//
//    ) {
//
//        waitForObfuscator(timeoutInMilliseconds)
//
//        val obfuscator = DefaultObfuscator.getStrategy()
//
//        Assert.assertNotNull(obfuscator)
//        Assert.assertEquals(expectedObfuscatorIdentity, obfuscator.name())
//
//        var salt = getSalt()
//        val id = salt?.identifier
//        val hashCode = salt?.hashCode()
//
//        Assert.assertNotNull(id)
//        Assert.assertTrue((hashCode ?: 0) > 0)
//
//        // FIXME: Recheck these:
//        //        Assert.assertTrue(salt?.firstTimeObtained?.get() == true)
//        //        Assert.assertEquals(1, salt?.refreshCount?.get())
//        //        Assert.assertEquals(0, salt?.refreshSkipCount?.get())
//
//        Assert.assertEquals(expectedSalt, salt?.takeValue())
//
//        salt = getSalt()
//
//        Assert.assertEquals(id, salt?.identifier)
//        Assert.assertEquals(hashCode, salt?.hashCode())
//
//        // FIXME: Recheck these:
//        //        Assert.assertTrue(salt?.fromCache() == true)
//        //        Assert.assertEquals(1, salt?.refreshCount?.get())
//        //        Assert.assertEquals(1, salt?.refreshSkipCount?.get())
//
//        Assert.assertEquals(expectedSalt, salt?.takeValue())
//
//        val obfuscatedResource = getObfuscatedData()
//        val obfuscated = expectedDeobfuscatedData.obfuscate()
//
//        Assert.assertTrue(isNotEmpty(obfuscated))
//
//        Assert.assertEquals(obfuscated, obfuscatedResource)
//
//        val data = getDeobfuscatedData()
//
//        Assert.assertEquals(expectedDeobfuscatedData, data)
//    }
}