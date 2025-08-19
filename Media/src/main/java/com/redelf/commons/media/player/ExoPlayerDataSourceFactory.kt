package com.redelf.commons.media.player

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.redelf.commons.extensions.keepAlive
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.syncOnWorkerJava
import com.redelf.commons.logging.Console
import com.redelf.commons.net.retrofit.RetryInterceptor
import com.redelf.commons.obtain.Obtain
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

@UnstableApi
class ExoPlayerDataSourceFactory : DataSource.Factory {

    companion object {

        private val pool = ConnectionPool(

            maxIdleConnections = 10,
            keepAliveDuration = 5,
            timeUnit = TimeUnit.MINUTES
        )

        val client = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectionPool(pool)
            .addInterceptor(HttpLoggingInterceptor())
            .addInterceptor(RetryInterceptor())
            .addNetworkInterceptor { chain ->

                val tag = "Exo :: Work manager wrapped :: Interceptor ::"

                Console.log("$tag START")

                val obtainer: Obtain<Response?> = object : Obtain<Response?> {

                    override fun obtain(): Response {

                        Console.log("$tag Obtaining response")

                        return chain.proceed(chain.request())
                    }
                }

                val result = syncOnWorkerJava(obtainer) ?: throw Exception("Null response")

                Console.log(

                    "$tag Response obtained :: Bytes=${result.body.contentLength()}"
                )

                if (result.isSuccessful) {

                    Console.log("$tag END :: ${result.code}")

                } else {

                    Console.error("$tag END :: ${result.code}")
                }

                result
            }
            .build()
    }

    override fun createDataSource(): DataSource {

        //        val cTimeout = 15_000
        //        val rTimeout = 30_000

        val cacheParameters = mapOf("User-Agent" to "ExoPlayer")

        //        val ctx = BaseApplication.takeContext()
        //
        //        if (ctx.isNotLegacyDevice()) {
        //
        //            return DefaultHttpDataSource.Factory()
        //                .setConnectTimeoutMs(cTimeout)
        //                .setReadTimeoutMs(rTimeout)
        //                .setAllowCrossProtocolRedirects(true)
        //                .setDefaultRequestProperties(cacheParameters)
        //                .createDataSource()
        //        }

        //        return ExoPlayerWorkManagerWrappedDataSource.Factory()
        //            .setConnectTimeoutMs(cTimeout)
        //            .setReadTimeoutMs(rTimeout)
        //            .setAllowCrossProtocolRedirects(true)
        //            .setDefaultRequestProperties(cacheParameters)
        //            .createDataSource()

        return OkHttpDataSource.Factory(client)
            .setDefaultRequestProperties(cacheParameters)
            .createDataSource()
    }
}