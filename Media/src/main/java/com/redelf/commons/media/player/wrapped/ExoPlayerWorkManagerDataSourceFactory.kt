package com.redelf.commons.media.player.wrapped

import android.os.Build
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource

@UnstableApi
class ExoPlayerWorkManagerDataSourceFactory : DataSource.Factory {

    //            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
    //
    //            DefaultHttpDataSource.Factory()
    //                .setConnectTimeoutMs(15000)
    //                .setReadTimeoutMs(30000)
    //                .setAllowCrossProtocolRedirects(true)
    //                .setDefaultRequestProperties(
    //
    //                    mapOf(
    //
    //                        "User-Agent" to "ExoPlayer",
    //                        "Cache-Control" to "max-stale=3600"
    //                    )
    //                )
    //
    //        } else {
    //
    //            ExoPlayerWorkManagerDataSourceFactory()
    //        }
    //
    //    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
    //        .setConnectTimeoutMs(30_000)               // Extended timeout for Doze
    //        .setReadTimeoutMs(60_000)                  // Longer read timeout
    //        .setAllowCrossProtocolRedirects(true)
    //        .setDefaultRequestProperties(
    //
    //            mapOf(
    //
    //                "User-Agent" to "ExoPlayer",
    //                "Cache-Control" to "max-stale=3600" // Cache-friendly
    //            )
    //        )
    //
    //    */

    override fun createDataSource(): DataSource {

        val cacheParameters = mapOf("User-Agent" to "ExoPlayer")

        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {

            DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(30000)
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(cacheParameters)
                .createDataSource()

        } else {

            ExoPlayerWorkManagerWrappedDataSource.Factory()
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(30000)
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(cacheParameters)
                .createDataSource()
        }
    }
}