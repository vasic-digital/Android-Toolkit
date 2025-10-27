/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.redelf.commons.net.content

import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.recordException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class RemoteHttpContentFetcher(

    private val endpoint: String,
    private val token: String = "",
    private val throwOnError: Boolean = false

) : RemoteContent<String> {

    @Throws(IOException::class, IllegalStateException::class)
    override fun fetch(): String {

        val data = fetchContentFromRemote(endpoint, token)

        data?.let {

            return it
        }

        return ""
    }

    @Throws(IOException::class, IllegalStateException::class)
    private fun fetchContentFromRemote(url: String, token: String): String? {

        /*
        * TODO: Retrofit
        */
        val client = OkHttpClient()

        val builder = Request.Builder().url(url)

        if (isNotEmpty(token)) {

            builder.addHeader("Authorization", "token $token")
        }

        val request = builder.build()

        return try {

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {

                response.body?.string()

            } else {

                val e = IOException("Failed to fetch content: ${response.code}")

                if (throwOnError) {

                    throw e
                }

                recordException(e)

                null
            }

        } catch (e: IOException) {

            if (throwOnError) {

                throw e
            }

            recordException(e)

            null
        }
    }
}