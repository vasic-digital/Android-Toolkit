package com.redelf.commons.media.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.execution.doze.DozeModeIOException
import com.redelf.commons.extensions.isDeviceInDozeMode
import com.redelf.commons.logging.Console
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

@UnstableApi
class ExoPlayerDataSourceFactory : DataSource.Factory {

    private val tag = "Exo :: Player Data Source ::"

    private val client by lazy { createDozeAwareOkHttpClient(BaseApplication.takeContext()) }

    override fun createDataSource(): DataSource {

        //        val cTimeout = 15_000
        //        val rTimeout = 30_000
        //        val cacheParameters = mapOf("User-Agent" to "ExoPlayer")

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
            .setDefaultRequestProperties(createDefaultHeaders())
            .setTransferListener(createTransferListener())
            .createDataSource()
    }

    private fun createDefaultHeaders(): Map<String, String> {

        return mapOf(

            "User-Agent" to "ExoPlayer/1.0",
            "Accept" to "audio/*, */*",
            "Cache-Control" to "max-stale=3600",
            "Connection" to "keep-alive"
        )
    }

    private fun createTransferListener(): TransferListener {

        val tag = "$tag Transfer Listener ::"

        return object : TransferListener {

            override fun onTransferInitializing(

                source: DataSource,
                dataSpec: DataSpec,
                isNetwork: Boolean

            ) {

                Console.log("$tag Transfer initializing: $isNetwork")
            }

            override fun onTransferStart(

                source: DataSource,
                dataSpec: DataSpec,
                isNetwork: Boolean

            ) {

                Console.log("$tag Transfer started: ${dataSpec.uri}")
            }

            override fun onTransferEnd(

                source: DataSource,
                dataSpec: DataSpec,
                isNetwork: Boolean

            ) {

                Console.log("$tag Transfer ended: ${dataSpec.uri}")
            }

            override fun onBytesTransferred(

                source: DataSource,
                dataSpec: DataSpec,
                isNetwork: Boolean,
                bytesTransferred: Int

            ) = Unit
        }
    }

    fun createDozeAwareOkHttpClient(context: Context): OkHttpClient {

        return OkHttpClient.Builder()
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .addInterceptor(createDozeAwareInterceptor(context))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            // Critical timeouts for Doze mode:
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // <-- Very long read timeout for Doze
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun createDozeAwareInterceptor(context: Context): Interceptor {

        return Interceptor { chain ->

            val request = chain.request()

            // Add headers to help with Doze mode
            val dozeAwareRequest = request.newBuilder()
                .header("Cache-Control", "max-stale=3600") // Cache for 1 hour
                .header("Connection", "keep-alive")
                .build()

            try {

                chain.proceed(dozeAwareRequest)

            } catch (e: IOException) {

                if (isDeviceInDozeMode(context)) {

                    throw DozeModeIOException("Device in Doze mode", e)
                }

                throw e
            }
        }
    }
}