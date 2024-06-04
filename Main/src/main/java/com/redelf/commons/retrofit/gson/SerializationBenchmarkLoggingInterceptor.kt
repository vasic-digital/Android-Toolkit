package com.redelf.commons.retrofit.gson

import com.redelf.commons.logging.Timber
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

            Timber.v("$tag Serializing time: $totalTime ms :: Url = ${request.url}")

        } else {

            Timber.w("$tag Serializing time: $totalTime ms, no body")
        }

        return response
    }
}