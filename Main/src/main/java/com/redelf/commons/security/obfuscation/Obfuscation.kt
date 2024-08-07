package com.redelf.commons.security.obfuscation

interface Obfuscation {

    fun identityObfuscator(): String

    fun obfuscate(input: String): String

    fun deobfuscate(input: String): String
}