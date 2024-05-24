package com.redelf.commons.retrofit.gson

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

class GsonLoggingInterceptor : Interceptor {

    private val tag = "GSON :: Interceptor ::"
    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()

        val startTime = System.currentTimeMillis()
        val response = chain.proceed(request)
        val endTime = System.currentTimeMillis()

        val totalTime = endTime - startTime

        if (response.body != null) {

            Timber.v("$tag Serializing time: $totalTime ms")

        } else {

            Timber.w("$tag Serializing time: $totalTime ms, no body")
        }

        return response
    }
}