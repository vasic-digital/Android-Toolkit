package com.redelf.commons.security.obfuscation

import com.redelf.commons.extensions.recordException
import com.redelf.commons.security.management.SecretsManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class RemoteObfuscatorSaltProvider(

    private val endpoint: String,
    private val token: String

) : ObfuscatorSaltProvider {

    companion object {

        private const val TAG = "RemoteObfuscatorSaltObtain ::"
    }

    fun getRemoteData(): String {

        val data = fetchContentFromPrivateRepo(endpoint, token)

        data?.let {

            return it
        }

        return ""
    }

    override fun obtain(): ObfuscatorSalt {

        return SecretsManager.obtain().getObfuscationSalt(this)
    }

    private fun fetchContentFromPrivateRepo(url: String, token: String): String? {

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "token $token")
            .build()

        return try {

            val response: Response = client.newCall(request).execute()

            if (response.isSuccessful) {

                response.body?.string()

            } else {

                val e = IOException("Failed to fetch content: ${response.code}")
                recordException(e)

                null
            }

        } catch (e: IOException) {

            recordException(e)
            null
        }
    }
}
