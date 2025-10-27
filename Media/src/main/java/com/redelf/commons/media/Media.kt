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