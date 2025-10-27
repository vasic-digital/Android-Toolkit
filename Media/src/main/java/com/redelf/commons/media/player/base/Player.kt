/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


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