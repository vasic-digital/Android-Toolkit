package com.redelf.commons.net.proxy.http

import android.content.Context
import com.redelf.commons.R
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.logging.Console
import com.redelf.commons.net.proxy.Proxy
import java.net.Authenticator
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MalformedURLException
import java.net.PasswordAuthentication
import java.net.URI
import java.net.URL
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class HttpProxy(

    ctx: Context,
    address: String, port: Int,

    var username: String? = null,
    var password: String? = null,

    val timeoutInMilliseconds: AtomicInteger = AtomicInteger(

        ctx.resources.getInteger(R.integer.proxy_timeout_in_milliseconds)
    ),

) : Proxy(address, port), TerminationSynchronized {

    companion object {

        var MEASUREMENT_ITERATIONS = 3

        val QUALITY_COMPARATOR =
            Comparator<HttpProxy> { p1, p2 -> p1.getQuality().compareTo(p2.getQuality()) }

        @Throws(IllegalArgumentException::class)
        private fun parseProxy(proxy: String): Triple<String, Int, Pair<String?, String?>> {

            return try {

                val url = URL(proxy)
                val address = url.host
                val port = url.port

                val userInfo = url.userInfo?.split(":")
                val username = userInfo?.getOrNull(0)
                val password = userInfo?.getOrNull(1)

                Triple(address, port, Pair(username, password))

            } catch (e: Exception) {

                Console.error(e)

                throw IllegalArgumentException("Could not pares proxy from URL: $proxy")
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    constructor(ctx: Context, proxy: String) : this(

        ctx,
        parseProxy(proxy).first,
        parseProxy(proxy).second,
        parseProxy(proxy).third.first,
        parseProxy(proxy).third.second
    )

    private val quality = AtomicLong(Long.MAX_VALUE)

    init {

        if (isNotEmpty(username) && isNotEmpty(password)) {

            Authenticator.setDefault(object : Authenticator() {

                override fun getPasswordAuthentication(): PasswordAuthentication {

                    return PasswordAuthentication(username, password?.toCharArray())
                }
            })
        }

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

    override fun ping(): Boolean {

        return try {

            val timeout = getTimeout()
            val inetAddress = InetAddress.getByName(address)

            inetAddress.isReachable(timeout)

        } catch (e: Exception) {

            Console.log(e)

            false
        }
    }

    override fun isAlive(ctx: Context): Boolean {

        return try {

            if (unreachable()) {

                return false
            }

            val proxy = get()
            val url = getTestUrl(ctx)

            val connection = url?.openConnection(proxy) as HttpURLConnection?

            connection?.requestMethod = "GET"
            connection?.readTimeout = timeoutInMilliseconds.get()
            connection?.connectTimeout = timeoutInMilliseconds.get()

            connection?.let {

                addAuthentication(it)
            }

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

            connection?.let {

                addAuthentication(it)
            }

            val startTime = System.currentTimeMillis()
            connection?.connect()

            val responseCode = connection?.responseCode
            val endTime = System.currentTimeMillis()

            connection?.disconnect()

            if (responseCode in 200..299) {

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

            val addressComparison = this.address.compareTo(other.address)

            if (addressComparison != 0) {

                return addressComparison
            }

            return this.port.compareTo(other.port)

        } else {

            throw IllegalArgumentException("Cannot compare HttpProxy with non-HttpProxy object")
        }
    }

    override fun equals(other: Any?): Boolean {

        if (other is HttpProxy) {

            return this.address == other.address && this.port == other.port
        }

        return super.equals(other)
    }

    override fun hashCode() = "$address:$port".hashCode()

    override fun terminate(): Boolean {

        try {

            Authenticator.setDefault(null)

            return true

        } catch (e: SecurityException) {

            Console.error(e)
        }

        return false
    }

    fun getUri(): URI? {

        try {

            return URI.create("http://$address:$port")

        } catch (e: IllegalArgumentException) {

            Console.error(e)
        }

        return null
    }

    fun getUrl(ctx: Context): URL? {

        try {

            return URL("http://$address:$port")

        } catch (e: MalformedURLException) {

            Console.error(e)
        }

        return null
    }

    private fun getTestUrl(ctx: Context): URL? {

        try {

            return URL(ctx.getString(R.string.proxy_alive_check_url))

        } catch (e: MalformedURLException) {

            Console.error(e)
        }

        return null
    }

    @Throws(IllegalArgumentException::class)
    private fun addAuthentication(connection: HttpURLConnection) {

        if (isNotEmpty(username) && isNotEmpty(password)) {

            val credentials = "$username:$password"
            val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray())

            connection.setRequestProperty("Proxy-Authorization", "Basic $encoded")

            Console.log("Added Proxy-Authorization header: Basic $encoded")
        }
    }

    private fun unreachable(): Boolean {

        return !ping()
    }
}