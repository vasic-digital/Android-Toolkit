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