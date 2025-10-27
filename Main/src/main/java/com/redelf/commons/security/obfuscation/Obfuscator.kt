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


package com.redelf.commons.security.obfuscation

import com.redelf.commons.obtain.OnObtain
import com.redelf.jcommons.JObfuscator


class Obfuscator(saltProvider: ObfuscatorSaltProvider) : SaltedObfuscator(saltProvider) {

    override fun obfuscate(input: String, callback: OnObtain<String>) {

        saltProvider.obtain(

            object : OnObtain<ObfuscatorSalt?> {

                override fun onCompleted(data: ObfuscatorSalt?) {

                    try {

                        val salt =  data?.takeValue() ?: ""
                        val jObfuscator = JObfuscator(salt)

                        val result = jObfuscator.obfuscate(input)

                        callback.onCompleted(result)

                    } catch (e: Throwable) {

                        callback.onFailure(e)
                    }
                }

                override fun onFailure(error: Throwable) {

                    callback.onFailure(error)
                }
            }
        )
    }

    override fun deobfuscate(input: String, callback: OnObtain<String>) {

        saltProvider.obtain(

            object : OnObtain<ObfuscatorSalt?> {

                override fun onCompleted(data: ObfuscatorSalt?) {

                    try {

                        val salt =  data?.takeValue() ?: ""
                        val jObfuscator = JObfuscator(salt)

                        val result = jObfuscator.deobfuscate(input)

                        callback.onCompleted(result)

                    } catch (e: Throwable) {

                        callback.onFailure(e)
                    }
                }

                override fun onFailure(error: Throwable) {

                    callback.onFailure(error)
                }
            }
        )
    }

    override fun name(callback: OnObtain<String>) {

        saltProvider.obtain(

            object : OnObtain<ObfuscatorSalt?> {

                override fun onCompleted(data: ObfuscatorSalt?) {

                    try {

                        val salt =  data?.takeValue() ?: ""
                        val jObfuscator = JObfuscator(salt)

                        val result = jObfuscator.name()

                        callback.onCompleted(result)

                    } catch (e: Throwable) {

                        callback.onFailure(e)
                    }
                }

                override fun onFailure(error: Throwable) {

                    callback.onFailure(error)
                }
            }
        )
    }
}