package com.redelf.commons.security.obfuscation

import java.util.concurrent.atomic.AtomicBoolean

object DefaultObfuscator : Obfuscation {

    val READY = AtomicBoolean(true)

    private var STRATEGY: SaltedObfuscator = Obfuscator(salt = "default_salt")

    fun setStrategy(strategy: SaltedObfuscator) {

        STRATEGY = strategy
    }

    fun getStrategy() = STRATEGY

    override fun obfuscate(input: String): String {

        return STRATEGY.obfuscate(input)
    }

    override fun deobfuscate(input: String): String {

        return STRATEGY.deobfuscate(input)
    }
}