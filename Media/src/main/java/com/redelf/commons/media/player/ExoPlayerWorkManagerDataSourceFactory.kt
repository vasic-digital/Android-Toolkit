package com.redelf.commons.media.player

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource

@UnstableApi
class ExoPlayerWorkManagerDataSourceFactory : DataSource.Factory {

    override fun createDataSource(): DataSource {

        return ExoPlayerWorkManagerDataSource()
    }
}