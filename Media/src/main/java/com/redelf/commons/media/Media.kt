package com.redelf.commons.media

import java.util.UUID

interface Media {

    fun onEnded()

    fun onSkipped()

    fun onStopped()

    fun onError(error: Throwable)

    fun onPaused()

    fun onStarted()

    fun onResumed()

    fun onProgress(position: Long, bufferedPosition: Int)

    fun autoPlayAsNextReady(): Boolean

    fun playAsPreviousReady(): Boolean

    fun getIdentifier(): UUID?

    fun getStreamUrl(): String?

    fun getShareUrl(): String?

    fun getCoverImage(): String?

    fun getDuration(): Long

    fun getCopyRight(): String?

    fun getImageGallery(): List<String>

    fun getTitle(): String?

    fun getSubtitle(): String?

    fun getMainTitle(): String?

    fun getParentPlaylist(): List<Media>?

    fun invokeCopyRights(): Boolean

    fun invokeImageGallery(): Boolean
}