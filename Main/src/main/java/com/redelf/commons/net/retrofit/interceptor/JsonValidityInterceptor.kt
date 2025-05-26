package com.redelf.commons.net.retrofit.interceptor

import com.google.gson.Gson
import com.google.gson.stream.MalformedJsonException
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import java.nio.charset.Charset

class JsonValidityInterceptor : Interceptor {

    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()
        val response = chain.proceed(request)

        val responseBody = response.body ?: return response

        val source = responseBody.source()

        source.request(Long.MAX_VALUE)

        val buffer = source.buffer

        val originalResponseString = buffer.clone().readString(Charset.forName("UTF-8"))

        try {

            gson.fromJson(originalResponseString, Any::class.java)

        } catch (e: Exception) {

            recordException(e)

            val err = IllegalArgumentException(

                "Invalid JSON data received from the API: $originalResponseString, " +
                        "Parent issue = ${e.message}"
            )

            recordException(err)
        }

        val newResponseBody = ResponseBody.create(responseBody.contentType(), originalResponseString)

        return response.newBuilder()
            .body(newResponseBody)
            .build()
    }
}