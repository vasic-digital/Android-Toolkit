package com.redelf.commons.net.proxy.http

import android.content.Context
import android.content.res.Resources.NotFoundException
import com.redelf.commons.R
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.net.proxy.Proxy
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.net.Proxy as JavaNetProxy

class HttpProxy(

    ctx: Context,

    schema: String,
    address: String,
    port: Int,

    var username: String? = null,
    var password: String? = null,

    private val testUrlResourceId: Int = R.string.proxy_alive_check_url,
    private val timeoutResourceId: Int = R.integer.proxy_timeout_in_milliseconds,
    private val timeoutInMilliseconds: AtomicInteger = getInteger(ctx, timeoutResourceId)

) : Proxy(schema, address, port) {

    companion object {

        var DEFAULT_TIMEOUT = 5000
        var MEASUREMENT_ITERATIONS = 3
        var DEFAULT_TEST_URL = "https://www.github.com"

        val QUALITY_COMPARATOR = Comparator<HttpProxy> { p1, p2 ->

            p1.getQuality().compareTo(p2.getQuality())
        }

        private fun getInteger(ctx: Context, resId: Int): AtomicInteger {

            val value = try {
                ctx.resources.getInteger(resId)
            } catch (e: NotFoundException) {

                recordException(e)

                DEFAULT_TIMEOUT
            }

            return AtomicInteger(value)
        }

        @Throws(IllegalArgumentException::class)
        private fun parseProxy(proxy: String): URL {

            return try {

                URL(proxy)

            } catch (e: Exception) {

                Console.error(e)

                throw IllegalArgumentException("Could not parse proxy from URL: $proxy")
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    constructor(ctx: Context, proxy: String) : this(

        ctx = ctx,
        port = parseProxy(proxy).port,
        address = parseProxy(proxy).host,
        schema = parseProxy(proxy).protocol.lowercase(),
        username = parseProxy(proxy).userInfo?.split(":")?.get(0),
        password = parseProxy(proxy).userInfo?.split(":")?.get(1)
    )

    private val quality = AtomicLong(Long.MAX_VALUE)

    init {

        var qSum = 0L

        for (i in 0 until MEASUREMENT_ITERATIONS) {

            val speedInMilliseconds = getSpeed(ctx)

            qSum += speedInMilliseconds
        }

        val newQuality: Long = qSum / MEASUREMENT_ITERATIONS

        quality.set(newQuality)
    }

    fun get(): JavaNetProxy {

        return JavaNetProxy(JavaNetProxy.Type.HTTP, InetSocketAddress(address, port))
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

        if (unreachable()) {

            return false
        }

        return getSpeed(ctx) != Long.MAX_VALUE
    }

    override fun getSpeed(ctx: Context): Long {
        return try {

            val testUrl = getTestUrl(ctx)
            val client = createOkHttpClient()

            if (testUrl == null) {

                throw IllegalArgumentException("Test URL is null or empty")
            }

            val request = Request.Builder()
                .url(testUrl)
                .build()

            val startTime = System.currentTimeMillis()
            val response: Response = client.newCall(request).execute()

            response.close()

            if (response.isSuccessful) {

                System.currentTimeMillis() - startTime

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

    fun getUri(): URI? {

        return try {

            URI.create("$schema://$address:$port")

        } catch (e: IllegalArgumentException) {

            Console.error(e)

            null
        }
    }

    fun getUrl(ctx: Context): URL? {

        return try {

            URL("$schema://$address:$port")

        } catch (e: MalformedURLException) {

            Console.error(e)

            null
        }
    }

    private fun getTestUrl(ctx: Context): URL? {

        return try {

            URL(ctx.getString(testUrlResourceId))

        } catch (e: Exception) {

            Console.error(e)

            null
        }
    }

    private fun createOkHttpClient(): OkHttpClient {

        val builder = OkHttpClient.Builder()
            .proxy(get())
            .connectTimeout(timeoutInMilliseconds.get().toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(timeoutInMilliseconds.get().toLong(), TimeUnit.MILLISECONDS)

        if (isNotEmpty(username) && isNotEmpty(password)) {

            val credentials = Credentials.basic(username ?: "", password ?: "")

            builder.proxyAuthenticator { _, response ->

                response.request.newBuilder()
                    .header("Proxy-Authorization", credentials)
                    .build()
            }
        }

        return builder.build()
    }

    private fun unreachable(): Boolean {

        return !ping()
    }
}