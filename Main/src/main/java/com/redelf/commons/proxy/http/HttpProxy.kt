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

class HttpProxy(address: String, port: Int) : Proxy(address, port) {

    private val quality = AtomicInteger(Int.MAX_VALUE)

    fun get(): java.net.Proxy {

        return java.net.Proxy(java.net.Proxy.Type.HTTP, InetSocketAddress(address, port))
    }

    override fun isAlive(ctx: Context): Boolean {

        return try {

            val proxy = get()
            val url = getTestUrl(ctx)

            val connection = url?.openConnection(proxy) as HttpURLConnection?

            connection?.requestMethod = "GET"
            connection?.readTimeout = 5000 // 5 seconds timeout
            connection?.connectTimeout = 5000 // 5 seconds timeout

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

            connection?.readTimeout = 5000 // 5 seconds timeout
            connection?.connectTimeout = 5000 // 5 seconds timeout
            connection?.requestMethod = "GET"

            val startTime = System.currentTimeMillis()
            connection?.connect()

            val responseCode = connection?.responseCode
            val endTime = System.currentTimeMillis()

            connection?.disconnect()

            if (responseCode == 200) {

                endTime - startTime
            } else {

                Long.MAX_VALUE // If the response is not 200, consider it as a very slow proxy
            }

        } catch (e: Exception) {

            Console.error(e)

            Long.MAX_VALUE // If any exception occurs, consider it as a very slow proxy
        }
    }

    override fun getQuality() = quality.get()

    private fun getTestUrl(ctx: Context): URL? {

        try {

            return URL(ctx.getString(R.string.proxy_alive_check_url))

        } catch (e: MalformedURLException) {

            Console.error(e)
        }

        return null
    }
}