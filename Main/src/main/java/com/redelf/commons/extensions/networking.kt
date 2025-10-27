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

        val tOut = if (ctx.isInBackground()) {

            5 * 1000L

        } else {

            60 * 1000L
        }

        Console.warning(

            "$tag NO INTERNET CONNECTION :: Waiting for it (timeout=${tOut}ms)".trim()
        )

        yieldWhile(

            timeoutInMilliseconds = tOut

        ) {

            notConnected()
        }

        if (notConnected()) {

            val msg = "$tag NO INTERNET CONNECTION :: Waiting timed-out (timeout=${tOut}ms)"
            val e = IOException(msg)
            recordException(e)

        } else {

            val msg = "$tag INTERNET CONNECTION AVAILABLE :: " +
                    "Waiting was not timed-out (timeout=${tOut}ms)"

            Console.debug(msg)
        }
    }

    return execute()
}