package com.redelf.commons.security.obfuscation

import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.security.management.SecretsManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class RemoteObfuscatorSaltObtain(

    private val endpoint: String,
    private val token: String

) : ObfuscatorSaltObtain {

    fun getRemoteData() : String {

        val data = fetchContentFromPrivateRepo(endpoint, token)

        data?.let {

            return it
        }

        return ""
    }

    override fun obtain(): String {

        try {

            val secrets = SecretsManager.obtain().obtain()

            secrets?.let {

                it.obfuscationSalt?.let { salt ->

                    if (isNotEmpty(salt)) {

                        SecretsManager.obtain().setObfuscationSalt(this)

                        return salt
                    }
                }
            }

        } catch (e: Exception) {

            recordException(e)
        }

        return getRemoteData()
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
