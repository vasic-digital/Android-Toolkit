package com.redelf.commons.test

class ObfuscatorTest : BaseTest() {

    // FIXME: Fix the test

//    @Test
//    fun testObfuscation() {
//
//        val saltProvider = object : ObfuscatorSaltProvider {
//
//            override fun obtain() = ObfuscatorSalt(value = "t3sR_s@lt!")
//        }
//
//        val obfuscator = Obfuscator(saltProvider)
//
//        listOf(
//
//            "test",
//            "TeSt",
//            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum vel enim at nisi commodo dignissim.",
//            "1234567890....1234567890"
//
//        ).forEach { input ->
//
//            val obfuscated = obfuscator.obfuscate(input)
//
//            Assert.assertTrue(isNotEmpty(obfuscated))
//            Assert.assertNotEquals(input, obfuscated)
//            Assert.assertTrue(input.length < obfuscated.length)
//
//            val deobfuscated = obfuscator.deobfuscate(obfuscated)
//
//            Assert.assertEquals(input, deobfuscated)
//        }
//    }
}