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