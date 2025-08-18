package com.redelf.commons.media.player.wrapped

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource

@UnstableApi
class ExoPlayerWorkManagerDataSourceFactory : DataSource.Factory {

    override fun createDataSource(): DataSource {

        val cacheParameters = mapOf("User-Agent" to "ExoPlayer")

        return ExoPlayerWorkManagerWrappedDataSource.Factory()
            .setConnectTimeoutMs(30_000)               // Extended timeout for Doze
            .setReadTimeoutMs(60_000)                  // Longer read timeout
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(cacheParameters)
            .createDataSource()
    }
}