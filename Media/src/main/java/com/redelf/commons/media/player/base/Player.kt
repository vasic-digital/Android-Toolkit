package com.redelf.commons.media.player.base

import com.redelf.commons.media.Media

interface Player {

    fun playAsync()

    fun play(): Boolean

    fun play(what: Media): Boolean

    fun assign(what: Media): Boolean

    fun play(what: List<Media>): Boolean

    fun assign(what: List<Media>): Boolean

    fun play(what: List<Media>, index: Int): Boolean

    fun assign(what: List<Media>, index: Int): Boolean

    fun play(afterSeconds: Int): Boolean

    fun play(what: List<Media>, index: Int, startFrom: Int): Boolean

    fun stop()

    fun stop(afterSeconds: Int): Boolean

    fun pause()

    fun pause(afterSeconds: Int): Boolean

    fun resume(): Boolean

    fun reset()

    fun seekTo(positionInSeconds: Int): Boolean

    fun seekTo(positionInMilliseconds: Float): Boolean

    fun getDuration(): Long

    fun getCurrentPosition(): Int

    fun isPlaying(): Boolean

    fun isNotPlaying(): Boolean

    fun onProgressChanged(position: Long, bufferedPosition: Int)

    fun getSpeed(): Float

    fun setSpeed(value: Float): Boolean

    fun getVolume(): Float

    fun setVolume(value: Float): Boolean

    fun resetSpeed(): Boolean

    fun cast(): Boolean

    fun share(): Boolean

    fun share(what: Media): Boolean

    fun toggleAutoPlay(): Boolean

    fun isAutoPlayOn(): Boolean

    fun isAutoPlayOff(): Boolean

    fun setAutoPlay(on: Boolean): Boolean

    fun next(): Boolean

    fun previous(): Boolean

    fun hasNext(): Boolean

    fun hasPrevious(): Boolean

    fun canNext(): Boolean

    fun canPrevious(): Boolean

    fun current(): Media?

    fun getPlayableItems(): List<Media>

    fun invokeCopyRights(): Boolean

    fun invokeImageGallery(): Boolean
}