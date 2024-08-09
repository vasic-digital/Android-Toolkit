package com.redelf.commons.net.endpoint.http

import android.content.Context
import com.google.android.gms.net.CronetProviderInstaller
import com.redelf.commons.R
import com.redelf.commons.execution.Executor
import com.redelf.commons.logging.Console
import com.redelf.commons.net.endpoint.Endpoint
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
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

        } catch (e: Exception) {

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

        } catch (e: Exception) {

            Console.log(e)

            false
        }
    }

    override fun getSpeed(ctx: Context): Long {

        return try {

            val url = getUrl()

            val builder = OkHttpClient.Builder()
                .readTimeout(timeoutInMilliseconds.get().toLong(), TimeUnit.MILLISECONDS)
                .connectTimeout(timeoutInMilliseconds.get().toLong(), TimeUnit.MILLISECONDS)

            val latch = CountDownLatch(1)

            CronetProviderInstaller.installProvider(ctx).addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    // TODO: Make sure that Cronet engine is shared
                    val cronetEngine = CronetEngine.Builder(ctx).build()

                    val cronetInterceptor = object : Interceptor {

                        @Throws(IOException::class)
                        private fun proceedWithCronet(request: Request, call: Call): Response {

                            val responseBuilder = Response.Builder()

                            // TODO: Build the response using Cronet engine
                            val callback = object : UrlRequest.Callback() {

                                override fun onRedirectReceived(

                                    request: UrlRequest?,
                                    info: UrlResponseInfo?,
                                    newLocationUrl: String?

                                ) {

                                    TODO("Not yet implemented")
                                }

                                override fun onResponseStarted(

                                    request: UrlRequest?,
                                    info: UrlResponseInfo?

                                ) {

                                    TODO("Not yet implemented")
                                }

                                override fun onReadCompleted(

                                    request: UrlRequest?,
                                    info: UrlResponseInfo?,
                                    byteBuffer: ByteBuffer?

                                ) {

                                    TODO("Not yet implemented")
                                }

                                override fun onSucceeded(

                                    request: UrlRequest?,
                                    info: UrlResponseInfo?

                                ) {

                                    TODO("Not yet implemented")
                                }

                                override fun onFailed(

                                    request: UrlRequest?,
                                    info: UrlResponseInfo?,
                                    error: CronetException?

                                ) {

                                    TODO("Not yet implemented")
                                }
                            }

                            val urlRequest = cronetEngine.newUrlRequestBuilder(

                                request.url.toString(),
                                callback,
                                Executor.MAIN.getPerformer()

                            ).build()

                            urlRequest.start()

                            return responseBuilder.build()
                        }

                        override fun intercept(chain: Interceptor.Chain): Response {

                            return proceedWithCronet(chain.request(), chain.call());
                        }
                    }

                    builder.addInterceptor(cronetInterceptor)
                }

                latch.countDown()
            }

            latch.await(5, TimeUnit.SECONDS)

            val client = builder.build()

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

        } catch (e: Exception) {

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

    override fun hashCode() : Int {

        val thisUri = getUri()

        return thisUri?.hashCode()?: 0
    }

    fun getUri(): URI? {

        try {

            val addressUrl = URL(address)

            return addressUrl.toURI()?.normalize()?.let { uri ->

                val normalizedPath = if (uri.path.endsWith("/")) uri.path.dropLast(1) else uri.path
                val normalizedPort = if (uri.port == 80) -1 else uri.port

                URI(uri.scheme, uri.userInfo, uri.host, normalizedPort, normalizedPath, uri.query, uri.fragment)
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