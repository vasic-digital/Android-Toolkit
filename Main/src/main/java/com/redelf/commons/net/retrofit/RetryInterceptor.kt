package com.redelf.commons.net.retrofit

import com.redelf.commons.logging.Console
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class RetryInterceptor : Interceptor {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    private val maxRetries = 2
    private val retryDelays = listOf(5000L, 10000L)

    init {

        if (DEBUG.get()) {

            Console.log(

                "Interceptor :: Retry :: Init :: " +
                        "Max retries = $maxRetries, Delays = $retryDelays"
            )
        }
    }

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