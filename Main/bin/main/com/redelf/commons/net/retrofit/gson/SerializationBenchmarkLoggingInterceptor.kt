package com.redelf.commons.net.retrofit.gson

import com.redelf.commons.logging.Console
import okhttp3.Interceptor
import okhttp3.Response

class SerializationBenchmarkLoggingInterceptor : Interceptor {

    private val tag = "Serialization benchmark :: Interceptor ::"
    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()

        val startTime = System.currentTimeMillis()
        val response = chain.proceed(request)
        val endTime = System.currentTimeMillis()

        val totalTime = endTime - startTime

        if (response.body != null) {

            Console.log("$tag Serializing time: $totalTime ms :: Url = ${request.url}")

        } else {

            Console.warning("$tag Serializing time: $totalTime ms, no body")
        }

        return response
    }
}