package com.redelf.commons.security.obfuscation

object DefaultObfuscator : Obfuscation {

    private var STRATEGY: Obfuscation = Obfuscator(salt = "default_salt")

    fun setStrategy(strategy: Obfuscation) {

        STRATEGY = strategy
    }

    override fun obfuscate(input: String): String {

        return STRATEGY.obfuscate(input)
    }

    override fun deobfuscate(input: String): String {

        return STRATEGY.deobfuscate(input)
    }
}