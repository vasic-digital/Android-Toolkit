package com.redelf.commons.security.obfuscation

import java.util.concurrent.atomic.AtomicBoolean

object DefaultObfuscator : Obfuscation {

    private val READY = AtomicBoolean()
    private var STRATEGY: SaltedObfuscator = Obfuscator(salt = ObfuscatorSalt())

    fun isReady() = READY.get()

    fun isNotReady() = !isReady()

    fun setReady(ready: Boolean) = READY.set(ready)

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