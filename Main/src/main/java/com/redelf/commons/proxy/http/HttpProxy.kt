package com.redelf.commons.proxy.http

import android.content.Context
import com.redelf.commons.R
import com.redelf.commons.logging.Console
import com.redelf.commons.proxy.Proxy
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class HttpProxy(

    ctx: Context,
    address: String, port: Int,

    private var timeoutInMilliseconds: AtomicInteger = AtomicInteger(

        ctx.resources.getInteger(R.integer.proxy_timeout_in_milliseconds)
    )

) : Proxy(address, port) {

    companion object {

        var MEASUREMENT_ITERATIONS = 3

        val QUALITY_COMPARATOR =
            Comparator<HttpProxy> { p1, p2 -> p1.getQuality().compareTo(p2.getQuality()) }

        @Throws(IllegalArgumentException::class)
        private fun parseProxy(proxy: String): Pair<String, Int> {

            return try {

                val url = URL(proxy)
                val address = url.host
                val port = url.port

                Pair(address, port)

            } catch (e: Exception) {

                Console.error(e)

                throw IllegalArgumentException("Could not pares proxy from URL: $proxy")
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    constructor(ctx: Context, proxy: String) :
            this(ctx, parseProxy(proxy).first, parseProxy(proxy).second)

    private val quality = AtomicLong(Long.MAX_VALUE)

    init {

        var qSum = 0L

        for (i in 0 until MEASUREMENT_ITERATIONS) {

            qSum += getSpeed(ctx)
        }

        quality.set(qSum / MEASUREMENT_ITERATIONS)
    }

    fun get(): java.net.Proxy {

        return java.net.Proxy(java.net.Proxy.Type.HTTP, InetSocketAddress(address, port))
    }

    override fun getTimeout() = timeoutInMilliseconds.get()

    override fun setTimeout(value: Int) {

        timeoutInMilliseconds.set(value)
    }

    override fun isAlive(ctx: Context): Boolean {

        return try {

            val proxy = get()
            val url = getTestUrl(ctx)

            val connection = url?.openConnection(proxy) as HttpURLConnection?

            connection?.requestMethod = "GET"
            connection?.readTimeout = timeoutInMilliseconds.get()
            connection?.connectTimeout = timeoutInMilliseconds.get()

            connection?.connect()

            val responseCode: Int = connection?.responseCode ?: -1
            connection?.disconnect()

            responseCode in 200..299

        } catch (e: Exception) {

            Console.log(e)

            false
        }
    }

    override fun getSpeed(ctx: Context): Long {

        return try {

            val proxy = get()
            val url = getTestUrl(ctx)
            val connection = url?.openConnection(proxy) as HttpURLConnection?

            connection?.readTimeout = timeoutInMilliseconds.get()
            connection?.connectTimeout = timeoutInMilliseconds.get()
            connection?.requestMethod = "GET"

            val startTime = System.currentTimeMillis()
            connection?.connect()

            val responseCode = connection?.responseCode
            val endTime = System.currentTimeMillis()

            connection?.disconnect()

            if (responseCode == 200) {

                endTime - startTime

            } else {

                Long.MAX_VALUE
            }

        } catch (e: Exception) {

            Console.error(e)

            Long.MAX_VALUE
        }
    }

    override fun getQuality() = quality.get()

    @Throws(IllegalArgumentException::class)
    override fun compareTo(other: Proxy): Int {

        if (other is HttpProxy) {

            // Compare addresses lexicographically
            val addressComparison = this.address.compareTo(other.address)

            if (addressComparison != 0) {

                return addressComparison
            }

            // If addresses are equal, compare ports numerically
            return this.port.compareTo(other.port)

        }  else {

            throw IllegalArgumentException("Cannot compare HttpProxy with non-HttpProxy object")
        }
    }

    override fun equals(other: Any?): Boolean {

        if (other is HttpProxy) {

            return this.address == other.address && this.port == other.port
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {

        return "$address:$port".hashCode()
    }

    private fun getTestUrl(ctx: Context): URL? {

        try {

            return URL(ctx.getString(R.string.proxy_alive_check_url))

        } catch (e: MalformedURLException) {

            Console.error(e)
        }

        return null
    }
}