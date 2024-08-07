package com.redelf.commons.security.obfuscation

interface Obfuscation {

    fun name(): String

    fun obfuscate(input: String): String

    fun deobfuscate(input: String): String
}