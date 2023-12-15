package com.redelf.commons.transmission.encryption

import com.redelf.commons.security.encryption.salt.SaltProvider

internal class DefaultSaltProvider(private val suffix: String = "S@1t") : SaltProvider<String> {

    override fun obtain(): String {

        return "${this@DefaultSaltProvider::class.simpleName}_@_$suffix"
    }
}