package com.redelf.commons.media.player.base

import com.redelf.commons.media.Media
import com.redelf.commons.obtain.OnObtain

interface PlayerAsync {

    fun playAsync()

    fun play(callback: OnObtain<Boolean>)

    fun play(what: Media, callback: OnObtain<Boolean>)

    fun assign(what: Media, callback: OnObtain<Boolean>)

    fun play(what: List<Media>, callback: OnObtain<Boolean>)

    fun assign(what: List<Media>, callback: OnObtain<Boolean>)

    fun play(what: List<Media>, index: Int, callback: OnObtain<Boolean>)

    fun assign(what: List<Media>, index: Int, callback: OnObtain<Boolean>)

    fun play(afterSeconds: Int, callback: OnObtain<Boolean>)

    fun play(what: List<Media>, index: Int, startFrom: Int, callback: OnObtain<Boolean>)

    fun stop()

    fun stop(afterSeconds: Int, callback: OnObtain<Boolean>)

    fun pause()

    fun pause(afterSeconds: Int, callback: OnObtain<Boolean>)

    fun resume(callback: OnObtain<Boolean>)

    fun reset()

    fun seekTo(positionInSeconds: Int, callback: OnObtain<Boolean>)

    fun seekTo(positionInMilliseconds: Float, callback: OnObtain<Boolean>)

    fun getDuration(): Long

    fun getCurrentPosition(): Int

    fun isPlaying(callback: OnObtain<Boolean>)

    fun isNotPlaying(callback: OnObtain<Boolean>)

    fun onProgressChanged(position: Long, bufferedPosition: Int)

    fun getSpeed(): Float

    fun setSpeed(value: Float, callback: OnObtain<Boolean>)

    fun getVolume(): Float

    fun setVolume(value: Float, callback: OnObtain<Boolean>)

    fun resetSpeed(callback: OnObtain<Boolean>)

    fun cast(callback: OnObtain<Boolean>)

    fun share(callback: OnObtain<Boolean>)

    fun share(what: Media, callback: OnObtain<Boolean>)

    fun toggleAutoPlay(callback: OnObtain<Boolean>)

    fun isAutoPlayOn(callback: OnObtain<Boolean>)

    fun isAutoPlayOff(callback: OnObtain<Boolean>)

    fun setAutoPlay(on: Boolean, callback: OnObtain<Boolean>)

    fun next(callback: OnObtain<Boolean>)

    fun previous(callback: OnObtain<Boolean>)

    fun hasNext(callback: OnObtain<Boolean>)

    fun hasPrevious(callback: OnObtain<Boolean>)

    fun canNext(callback: OnObtain<Boolean>)

    fun canPrevious(): Boolean

    fun current(): Media?

    fun getPlayableItems(): List<Media>

    fun invokeCopyRights(callback: OnObtain<Boolean>)

    fun invokeImageGallery(callback: OnObtain<Boolean>)
}