package com.redelf.commons.net.retrofit

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.extensions.exec
import com.redelf.commons.logging.Console
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class RetryInterceptor : Interceptor {

    companion object {

        val DEBUG = AtomicBoolean()

        const val TAG = "Interceptor :: Retry ::"

        const val BROADCAST_ACTION_COMMUNICATION_FAILURE =
            "RetryInterceptor.Broadcast.Action.Communication.Failure"
    }

    private val maxRetries = 2
    private val retryDelays = listOf(5000L, 10000L)
    private val msg1 = "Failed to execute request"
    private val msg2 = "Failed to execute request after $maxRetries retries"

    init {

        if (DEBUG.get()) {

            Console.log("$TAG Init :: Max retries = $maxRetries, Delays = $retryDelays")
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {

        var attempt = 0
        var response: Response? = null

        while (attempt <= maxRetries) {

            try {

                response = chain.proceed(chain.request())

                if (response.isSuccessful || response.code == 404) {

                    return response
                }

            } catch (e: IOException) {

                if (attempt == maxRetries) {

                    fail(msg2)

                    throw e

                } else {

                    fail(msg1)
                }
            }

            Thread.sleep(retryDelays.getOrElse(attempt) { 0L })
            attempt++
        }

        response?.let {

            return it
        }

        fail(msg2)

        throw IOException(msg2)
    }

    private fun fail(msg: String) = exec {

        Console.error("$TAG $msg")

        val intent = Intent(BROADCAST_ACTION_COMMUNICATION_FAILURE)
        val applicationContext = BaseApplication.takeContext()
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }
}