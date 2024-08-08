package com.redelf.commons.security.obfuscation

import com.redelf.commons.extensions.recordException
import com.redelf.commons.net.content.RemoteHttpContentFetcher
import com.redelf.commons.security.management.SecretsManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class RemoteObfuscatorSaltProvider(

    private val endpoint: String,
    private val token: String

) : ObfuscatorSaltProvider {

    fun getRemoteData(): String {

        return RemoteHttpContentFetcher(endpoint, token).fetch()
    }

    override fun obtain(): ObfuscatorSalt? {

        return SecretsManager.obtain().getObfuscationSalt(this)
    }
}
