package com.redelf.commons.media.player

import androidx.media3.exoplayer.ExoPlayer
import com.redelf.commons.execution.ExecuteWithResult
import com.redelf.commons.extensions.onUiThread
import com.redelf.commons.media.Media
import com.redelf.commons.media.Player
import java.util.concurrent.atomic.AtomicBoolean

abstract class PlayerAbstraction<MP> : Player, ExecuteWithResult<Media> {

    companion object {

        const val BROADCAST_EXTRA_STOP_CODE = 1
        const val BROADCAST_EXTRA_PLAY_CODE = 2
        const val BROADCAST_EXTRA_PAUSE_CODE = 3

        protected val prepared = AtomicBoolean()

        private var speed: Float = 1.0f
        private var media: Media? = null
        private var volume: Float = 1.0f
        private val playing = AtomicBoolean()
        private var mediaList: List<Media>? = null
    }

    protected open fun getMedia() = media

    protected open fun setMedia(item: Media) {

        media = item
    }

    protected open fun getMediaList() = mediaList

    protected open fun setMediaList(items: List<Media>) {

        mediaList = items
    }

    override fun getSpeed() = speed

    override fun getVolume() = volume

    protected open fun setSpeedValue(value: Float) {

        speed = value
    }

    protected open fun setVolumeValue(value: Float) {

        volume = value
    }

    protected abstract fun getMediaPlayer(): MP?

    protected abstract fun setMediaPlayer(value: MP)

    protected abstract fun unsetMediaPlayer()

    protected fun clearMediaPlayer() {

        prepared.set(false)

        unsetMediaPlayer()
    }

    protected open fun clearMediaItem() {

        media = null
    }

    protected open fun getPlaying() = playing.get()

    protected open fun setPlaying(value: Boolean) = playing.set(value)

    protected fun isPrepared() = prepared.get()

    protected fun setPrepared() = prepared.set(true)

    protected fun withPlayer(

        onUiThread: Boolean = true,
        doWhat: (ep: MP) -> Unit

    ) {

        getMediaPlayer()?.let { player ->

            onUiThread {

                doWhat(player)
            }
        }
    }
}