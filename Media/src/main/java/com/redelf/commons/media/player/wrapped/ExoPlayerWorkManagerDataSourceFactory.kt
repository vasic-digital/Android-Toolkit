package com.redelf.commons.media.player.wrapped

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import com.redelf.commons.application.BaseApplication

@UnstableApi
class ExoPlayerWorkManagerDataSourceFactory : DataSource.Factory {

    override fun createDataSource(): DataSource {

        val cTimeout = 15_000
        val rTimeout = 30_000
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

        return ExoPlayerWorkManagerWrappedDataSource.Factory()
            .setConnectTimeoutMs(cTimeout)
            .setReadTimeoutMs(rTimeout)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(cacheParameters)
            .createDataSource()
    }
}