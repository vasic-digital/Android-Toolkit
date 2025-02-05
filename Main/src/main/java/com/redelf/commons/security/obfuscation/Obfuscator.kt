package com.redelf.commons.security.obfuscation

import com.redelf.commons.extensions.recordException
import com.redelf.jcommons.JObfuscator


class Obfuscator(saltProvider: ObfuscatorSaltProvider) : SaltedObfuscator(saltProvider) {

    override fun obfuscate(input: String): String {

        try {

            val salt = saltProvider.obtain()?.takeValue() ?: ""
            val jObfuscator = JObfuscator(salt)

            return jObfuscator.obfuscate(input)

        } catch (e: Exception) {

            recordException(e)
        }

        return ""
    }

    override fun deobfuscate(input: String): String {

        try {

            val salt = saltProvider.obtain()?.takeValue() ?: ""
            val jObfuscator = JObfuscator(salt)

            return jObfuscator.deobfuscate(input)

        } catch (e: Exception) {

            recordException(e)
        }

        return ""
    }

    override fun name(): String {

        val salt = saltProvider.obtain()?.takeValue() ?: ""

        return JObfuscator(salt).name()
    }
}