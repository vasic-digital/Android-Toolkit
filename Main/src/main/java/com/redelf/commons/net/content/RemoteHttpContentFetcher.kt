package com.redelf.commons.net.content

import com.redelf.commons.extensions.recordException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class RemoteHttpContentFetcher(

    private val endpoint: String,
    private val token: String

) : RemoteContent<String> {

    override fun fetch(): String {

        val data = fetchContentFromRemote(endpoint, token)

        data?.let {

            return it
        }

        return ""
    }

    private fun fetchContentFromRemote(url: String, token: String): String? {

        /*
        * TODO: Retrofit
        */
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