package com.redelf.commons.extensions

import com.redelf.commons.application.BaseApplication
import com.redelf.commons.logging.Console
import com.redelf.commons.net.connectivity.Connectivity
import retrofit2.Call
import retrofit2.Response
import java.io.IOException

@Throws(IOException::class)
fun <T> Call<T>.executeConnected(

    tag: String = "",
    connectionCheckEndpoint: String = "www.google.com"

): Response<T?> {

    val ctx = BaseApplication.takeContext()
    val connectivity = Connectivity(endpoint = connectionCheckEndpoint)

    fun notConnected(): Boolean {

        return connectivity.isNetworkUnavailable(ctx)
    }

    if (notConnected()) {

        Console.warning(

            "$tag NO INTERNET CONNECTION :: Waiting for it".trim()
        )

        yieldWhile(

            timeoutInMilliseconds = 60 * 1000L

        ) {

            notConnected()
        }

        if (notConnected()) {

            val msg = "$tag NO INTERNET CONNECTION :: Waiting timeout"
            val e = IOException(msg)
            recordException(e)
        }
    }

    return execute()
}