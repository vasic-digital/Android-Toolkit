package com.redelf.commons.security.obfuscation

import android.util.Base64
import com.redelf.commons.extensions.recordException


class Obfuscator(salt: ObfuscatorSalt) : SaltedObfuscator(salt) {

    constructor(salt: ObfuscatorSaltObtain) : this(salt.obtain())

    override fun obfuscate(input: String): String {

        try {

            val saltedInput = input + salt

            val obfuscatedBytes = saltedInput.toByteArray().map { it.toInt() xor salt.value.hashCode() }
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

            val originalBytes = decodedBytes.map { it.toInt() xor salt.value.hashCode() }
                .map { it.toByte() }
                .toByteArray()

            val originalString = String(originalBytes)

            return originalString.removeSuffix(salt.value ?: "")

        } catch (e: Exception) {

            recordException(e)
        }

        return ""
    }
}