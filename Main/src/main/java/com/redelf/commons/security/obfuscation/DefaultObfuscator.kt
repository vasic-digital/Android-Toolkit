package com.redelf.commons.security.obfuscation

import com.redelf.commons.obtain.OnObtain
import java.util.concurrent.atomic.AtomicBoolean

object DefaultObfuscator : ObfuscationAsync {

    private val READY = AtomicBoolean()

    private val defaultSaltProvider = object : ObfuscatorSaltProvider {

        override fun obtain(callback: OnObtain<ObfuscatorSalt?>) {

            callback.onCompleted(ObfuscatorSalt())
        }
    }

    private var STRATEGY: SaltedObfuscator = Obfuscator(saltProvider = defaultSaltProvider)

    fun isReady() = READY.get()

    fun isNotReady() = !isReady()

    fun setReady(ready: Boolean) = READY.set(ready)

    fun setStrategy(strategy: SaltedObfuscator) {

        STRATEGY = strategy
    }

    fun getStrategy() = STRATEGY

    override fun obfuscate(input: String, callback: OnObtain<String>) {

        return STRATEGY.obfuscate(input, callback)
    }

    override fun deobfuscate(input: String, callback: OnObtain<String>) {

        return STRATEGY.deobfuscate(input, callback)
    }

    override fun name(callback: OnObtain<String>) {

        STRATEGY.name(callback)
    }
}