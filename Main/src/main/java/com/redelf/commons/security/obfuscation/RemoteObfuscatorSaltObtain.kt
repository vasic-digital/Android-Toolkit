package com.redelf.commons.security.obfuscation

import com.redelf.commons.execution.Executor
import com.redelf.commons.extensions.exec
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

    override fun obtain(): String {

        fun getData() : String {

            val data = fetchContentFromPrivateRepo(endpoint, token)

            data?.let {

                return it
            }

            return ""
        }

        try {

            val secrets = SecretsManager.obtain().obtain()

            secrets?.let {

                it.obfuscationSalt?.let { salt ->

                    if (isNotEmpty(salt)) {

                        exec {

                            try {

                                val newSalt = getData()

                                if (isNotEmpty(newSalt)) {

                                    it.obfuscationSalt = newSalt

                                    SecretsManager.obtain().pushData(it)
                                }

                            } catch (e: Exception) {

                                recordException(e)
                            }
                        }

                        return salt
                    }
                }
            }

        } catch (e: Exception) {

            recordException(e)
        }

        return getData()
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
