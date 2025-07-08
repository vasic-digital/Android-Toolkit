package com.redelf.commons.security.obfuscation

import com.redelf.commons.obtain.OnObtain

interface ObfuscationAsync {

    fun name(callback: OnObtain<String>)

    fun obfuscate(input: String, callback: OnObtain<String>)

    fun deobfuscate(input: String, callback: OnObtain<String>)
}