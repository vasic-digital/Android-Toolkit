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


package com.redelf.commons.net.endpoint.http

import android.content.Context
import com.redelf.commons.R
import com.redelf.commons.logging.Console
import com.redelf.commons.net.endpoint.Endpoint
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import useCronet
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong


class HttpEndpoint(

    ctx: Context,
    address: String,

    private var timeoutInMilliseconds: AtomicInteger = AtomicInteger(

        ctx.resources.getInteger(R.integer.endpoint_timeout_in_milliseconds)
    )

) : Endpoint(address) {

    companion object {

        var MEASUREMENT_ITERATIONS = 3

        val QUALITY_COMPARATOR =
            Comparator<HttpEndpoint> { p1, p2 -> p1.getQuality().compareTo(p2.getQuality()) }
    }

    private val quality = AtomicLong(Long.MAX_VALUE)

    init {

        var qSum = 0L

        for (i in 0 until MEASUREMENT_ITERATIONS) {

            qSum += getSpeed(ctx)
        }

        quality.set(qSum / MEASUREMENT_ITERATIONS)
    }

    override fun getTimeout() = timeoutInMilliseconds.get()

    override fun setTimeout(value: Int) {

        timeoutInMilliseconds.set(value)
    }

    override fun ping(): Boolean {

        return try {

            val timeout = getTimeout()
            val inetAddress = InetAddress.getByName(address)

            inetAddress.isReachable(timeout)

        } catch (e: Throwable) {

            Console.log(e)

            false
        }
    }

    override fun isAlive(ctx: Context): Boolean {

        return try {

            val url = getUrl()

            val connection = url?.openConnection() as HttpURLConnection?

            connection?.requestMethod = "GET"
            connection?.readTimeout = timeoutInMilliseconds.get()
            connection?.connectTimeout = timeoutInMilliseconds.get()

            connection?.connect()

            val responseCode: Int = connection?.responseCode ?: -1
            connection?.disconnect()

            responseCode in 200..299

        } catch (e: Throwable) {

            Console.log(e)

            false
        }
    }

    override fun getSpeed(ctx: Context): Long {

        return try {

            val url = getUrl()

            val client = OkHttpClient.Builder()
                .readTimeout(timeoutInMilliseconds.get().toLong(), TimeUnit.MILLISECONDS)
                .connectTimeout(timeoutInMilliseconds.get().toLong(), TimeUnit.MILLISECONDS)
                .useCronet()
                .build()

            if (url == null) {

                throw IllegalArgumentException("URL is null or empty")
            }

            val request = Request.Builder()
                .url(url)
                .build()

            val startTime = System.currentTimeMillis()
            val response: Response = client.newCall(request).execute()

            response.close()

            if (response.isSuccessful) {

                System.currentTimeMillis() - startTime

            } else {

                Long.MAX_VALUE
            }

        } catch (e: Throwable) {

            Console.error(e)

            Long.MAX_VALUE
        }
    }

    override fun getQuality() = quality.get()

    override fun compareTo(other: Endpoint): Int {

        return this.address.compareTo(other.address)
    }

    override fun equals(other: Any?): Boolean {

        if (other is HttpEndpoint) {

            val thisUri = this.getUri()
            val otherUri = other.getUri()

            return thisUri == otherUri
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {

        val thisUri = getUri()

        return thisUri?.hashCode() ?: 0
    }

    fun getUri(): URI? {

        try {

            val addressUrl = URL(address)

            return addressUrl.toURI()?.normalize()?.let { uri ->

                val normalizedPath = if (uri.path.endsWith("/")) uri.path.dropLast(1) else uri.path
                val normalizedPort = if (uri.port == 80) -1 else uri.port

                URI(
                    uri.scheme,
                    uri.userInfo,
                    uri.host,
                    normalizedPort,
                    normalizedPath,
                    uri.query,
                    uri.fragment
                )
            }

        } catch (e: MalformedURLException) {

            Console.error(e)
        }

        return null
    }

    fun getUrl(): URL? {

        try {

            val thisUri = getUri()

            return thisUri?.toURL()

        } catch (e: MalformedURLException) {

            Console.error(e)
        }

        return null
    }
}