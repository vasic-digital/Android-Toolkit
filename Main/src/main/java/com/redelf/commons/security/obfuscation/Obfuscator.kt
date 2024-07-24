package com.redelf.commons.security.obfuscation

import android.util.Base64
import com.redelf.commons.extensions.recordException


class Obfuscator(saltProvider: ObfuscatorSaltProvider) : SaltedObfuscator(saltProvider) {

    override fun obfuscate(input: String): String {

        try {

            val saltedInput = input + saltProvider

            val obfuscatedBytes = saltedInput.toByteArray()
                .map { it.toInt() xor saltProvider.obtain()?.takeValue().hashCode() }
                .map { it.toByte() }
                .toByteArray()

            return Base64.encodeToString(obfuscatedBytes, Base64.DEFAULT)

        } catch (e: Exception) {

            recordException(e)
        }

        return ""
    }

    override fun deobfuscate(input: String): String {

        try {

            val decodedBytes = Base64.decode(input, Base64.DEFAULT)

            val originalBytes = decodedBytes
                .map { it.toInt() xor saltProvider.obtain()?.takeValue().hashCode() }
                .map { it.toByte() }
                .toByteArray()

            val originalString = String(originalBytes)

            return originalString.removeSuffix(saltProvider.obtain()?.takeValue() ?: "")

        } catch (e: Exception) {

            recordException(e)
        }

        return ""
    }
}