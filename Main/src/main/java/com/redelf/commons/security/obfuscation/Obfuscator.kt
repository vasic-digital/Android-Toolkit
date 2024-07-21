package com.redelf.commons.security.obfuscation

import android.util.Base64


class Obfuscator(private val salt: String) {

    fun obfuscate(input: String): String {

        val saltedInput = input + salt
        val obfuscatedBytes = saltedInput.toByteArray().map { it.toInt() xor salt.hashCode() }.map { it.toByte() }.toByteArray()
        return Base64.encodeToString(obfuscatedBytes, Base64.DEFAULT)
    }

    fun deobfuscate(input: String): String {

        val decodedBytes = Base64.decode(input, Base64.DEFAULT)
        val originalBytes = decodedBytes.map { it.toInt() xor salt.hashCode() }.map { it.toByte() }.toByteArray()
        val originalString = String(originalBytes)

        return originalString.removeSuffix(salt)
    }
}