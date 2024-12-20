package com.redelf.commons.net.retrofit

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RetryInterceptor : Interceptor {

    private val maxRetries = 2
    private val retryDelays = listOf(5000L, 10000L)

    override fun intercept(chain: Interceptor.Chain): Response {

        var attempt = 0
        var response: Response? = null

        while (attempt <= maxRetries) {

            try {

                response = chain.proceed(chain.request())

                if (response.isSuccessful) {

                    return response
                }
            } catch (e: IOException) {

                if (attempt == maxRetries) {

                    throw e
                }
            }

            Thread.sleep(retryDelays.getOrElse(attempt) { 0L })
            attempt++
        }

        return response ?: throw IOException("Failed to execute request after $maxRetries retries")
    }
}