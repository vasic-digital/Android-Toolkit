package com.redelf.commons.security.obfuscation

import com.redelf.commons.extensions.exec
import com.redelf.commons.net.content.RemoteHttpContentFetcher
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.security.management.SecretsManager

class RemoteObfuscatorSaltProvider(

    private val endpoint: String,
    private val token: String

) : ObfuscatorSaltProvider {

    fun getRemoteData(): String {

        return RemoteHttpContentFetcher(endpoint, token).fetch()
    }

    override fun obtain(callback: OnObtain<ObfuscatorSalt?>) {

        exec {

            SecretsManager.obtain().getObfuscationSalt(this@RemoteObfuscatorSaltProvider, callback)
        }
    }
}
