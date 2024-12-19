package com.redelf.commons.media.player

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.execution.Retrying
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.onUiThread
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.media.Media
import java.io.IOException
import kotlin.collections.indexOf
import kotlin.text.toLong

typealias EPlayer = androidx.media3.exoplayer.ExoPlayer

abstract class ExoPlayer : PlayerAbstraction<EPlayer>() {

    companion object {

        private var exoPlayer: EPlayer? = null
    }

    private var currentDuration: Long = 0

    override fun reset() {

        clearMediaItem()
    }

    override fun isAutoPlayOff(): Boolean {

        return !isAutoPlayOn()
    }

    override fun playAsync() {

        Console.log("Play async")

        exec {

            play()
        }
    }

    override fun play(): Boolean {

        getMedia()?.let {

            return play(it)
        }

        return loadAndPlay()
    }

    override fun play(what: Media): Boolean {

        return play(listOf(what))
    }

    override fun play(what: List<Media>): Boolean {

        var index = 0

        getMedia()?.let {

            index = what.indexOf(it)

            if (index < 0) {

                index = 0
            }
        }

        return play(what, index)
    }

    override fun play(what: List<Media>, index: Int, startFrom: Int): Boolean {

        val logTag = "Player :: Play :: TO EXEC. ::"

        Console.log("$logTag ${what.size} :: $index :: $startFrom :: ${what[index].getIdentifier()}")

        if (isPlaying()) {

            destroyMediaPlayer()
        }

        setMediaList(what)
        setMedia(what[index])

        Console.log("$logTag Set: ${getMedia()?.getIdentifier()}")

        getMedia()?.let {

            Console.log("$logTag Execute: ${it.getIdentifier()}")

            return execute(it, startFrom)
        }

        Console.error("$logTag No playable item")

        return false
    }

    override fun play(what: List<Media>, index: Int): Boolean {

        return play(what, index, -1)
    }

    override fun assign(what: Media): Boolean {

        return assign(listOf(what))
    }

    override fun assign(what: List<Media>): Boolean {

        var index = 0

        getMedia()?.let {

            index = what.indexOf(it)

            if (index < 0) {

                index = 0
            }
        }

        return assign(what, index)
    }

    override fun assign(what: List<Media>, index: Int): Boolean {

        if (isPlaying()) {

            destroyMediaPlayer()
        }

        setMediaList(what)
        setMedia(what[index])

        return true
    }

    private fun execute(what: Media, startFrom: Int): Boolean {

        var result = false
        val logTag = "Player :: Play :: Execution :: ${what.getIdentifier()} ::"

        Console.log("$logTag Start :: from=$startFrom")


        val ePlayer = instantiateMediaPlayer()

        ePlayer?.let { ep ->

            Console.log("$logTag Player instantiated")

            try {

                ep.addListener(object : Player.Listener {

                    override fun onPlaybackStateChanged(state: Int) {

                        if (state == Player.STATE_ENDED) {

                            stop()

                            what.onEnded()

                            if (canNext()) {

                                next()
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {

                        val msg = "ExoPlayer error: ${error.errorCode}, extra: ${error.errorCodeName}"
                        val e = IllegalStateException(msg)

                        what.onError(e)
                        Console.error("$logTag Error: ${error.errorCode}")
                    }

                    override fun onPlaybackStateChanged(state: Int) {

                        if (state == Player.STATE_READY) {

                            setPrepared()
                            Console.log("$logTag Prepared")
                        }
                    }
                })

                if (!getPlaying()) {

                    exec(

                        onRejected = { err ->

                            recordException(err)
                        }

                    ) {

                        val streamUrl = what.getStreamUrl()

                        if (isEmpty(streamUrl)) {

                            Console.error("$logTag Empty stream url")
                        }

                        streamUrl?.let {

                            Console.log("$logTag Stream url: $streamUrl")

                            try {

                                val mediaItem = MediaItem.fromUri(streamUrl)
                                ep.setMediaItem(mediaItem)

                                applySpeed(ep)

                                val duration = doGetDuration()

                                setCurrentDuration(duration)

                                ep.playWhenReady = true

                                Console.log("$logTag START :: Duration = $duration")

                                startPublishingProgress(ep)

                                setPlaying(true)

                                Console.log("$logTag Playing set")

                                Console.log("$logTag Result set")

                                val currentProgress: Float = if (startFrom < 0) {

                                    obtainCurrentProgress(what)

                                } else {

                                    startFrom.toFloat()
                                }

                                currentProgress.let { progress ->

                                    Console.log("$logTag Progress obtained: $currentProgress")

                                    seekTo(progress.toInt())

                                    Console.log("$logTag Seek")
                                }

                                what.onStarted()

                                Console.log("$logTag On started")

                                if (!setVolume(1.0f)) {

                                    Console.warning("$logTag Could not set the volume")
                                }

                            } catch (e: IllegalStateException) {

                                Console.error(e)

                            } catch (e: IOException) {

                                Console.error(e)

                            } catch (e: Exception) {

                                recordException(e)
                            }
                        }
                    }

                    result = true

                } else {

                    Console.error("$logTag Already playing")
                }

            } catch (e: IOException) {

                Console.error("$logTag ${e::class.simpleName} :: ${e.message}")

            } catch (e: IllegalArgumentException) {

                Console.error("$logTag ${e::class.simpleName} :: ${e.message}")

            } catch (e: SecurityException) {

                Console.error("$logTag ${e::class.simpleName} :: ${e.message}")

            } catch (e: IllegalStateException) {

                Console.error("$logTag ${e::class.simpleName}")

                Console.error(e)

            } catch (e: Throwable) {

                recordException(e)
            }
        }

        Console.log("$logTag Result: $result")

        return result
    }

    override fun execute(what: Media): Boolean {

        return execute(what, -1)
    }

    override fun stop() {

        Console.log("stop()")

        destroyMediaPlayer()

        getMedia()?.onStopped()
    }

    override fun stop(afterSeconds: Int) = doAfter(BROADCAST_EXTRA_STOP_CODE, afterSeconds)

    override fun play(afterSeconds: Int) = doAfter(BROADCAST_EXTRA_PLAY_CODE, afterSeconds)

    override fun pause(afterSeconds: Int) = doAfter(BROADCAST_EXTRA_PAUSE_CODE, afterSeconds)

    override fun pause() {

        getMediaPlayer()?.let {

            if (getPlaying()) {

                try {

                    it.pause()
                    setPlaying(false)
                    getMedia()?.onPaused()

                } catch (e: IllegalStateException) {

                    Console.error(e.message)
                }
            }
        }
    }

    override fun resume(): Boolean {

        getMediaPlayer()?.let {

            if (!getPlaying()) {

                try {

                    it.playWhenReady = true
                    setPlaying(true)
                    startPublishingProgress(it)
                    getMedia()?.onResumed()

                    return true

                } catch (e: IllegalStateException) {

                    Console.warning(e.message)
                }
            }
        }

        return false
    }

    override fun seekTo(positionInMilliseconds: Float): Boolean {

        Console.log("Seek to: $positionInMilliseconds milliseconds")

        getMediaPlayer()?.let {

            try {

                Console.log("Seek to: $positionInMilliseconds")

                it.seekTo(positionInMilliseconds.toLong())

                Console.log("Seek to: $positionInMilliseconds done")

                return true

            } catch (e: IllegalStateException) {

                Console.warning(e.message)

            } catch (e: Exception) {

                Console.error(e)
            }
        }

        return false
    }

    override fun seekTo(positionInSeconds: Int): Boolean {

        Console.log("Seek to: $positionInSeconds seconds")

        return seekTo(positionInSeconds * 1000f)
    }

    override fun getDuration(): Long {

        val current = getCurrentDuration()

        if (current > 0) {

            return current
        }

        val d = doGetDuration()

        setCurrentDuration(d)

        return d
    }

    override fun getCurrentPosition(): Int {

        getMediaPlayer()?.let {

            if (isPrepared()) {

                try {

                    // TODO:
//                    return it.currentPosition

                } catch (e: IllegalStateException) {

                    Console.error(e)
                }
            }
        }

        return 0
    }

    override fun isPlaying() = getPlaying()

    override fun isNotPlaying() = !isPlaying()

    override fun onProgressChanged(position: Long, bufferedPosition: Int) {

        getMedia()?.onProgress(position, bufferedPosition)
    }

    override fun setSpeed(value: Float): Boolean {

        setSpeedValue(value)

        val tag = "SPEED :: SET ::"

        Console.log("$tag To: ${getSpeed()}")

        return applySpeed()
    }

    override fun setVolume(value: Float): Boolean {

        setVolumeValue(value)

        val tag = "VOLUME :: SET ::"

        Console.log("$tag To: ${getVolume()}")

        return applyVolume()
    }

    override fun resetSpeed(): Boolean {

        val tag = "SPEED :: RESET ::"

        val toSpeed = 1f
        val reset = setSpeed(toSpeed)

        if (reset) {

            Console.log("$tag To: ${getSpeed()}")

        } else {

            Console.warning("$tag Failed")
        }

        return reset
    }

    override fun canNext(): Boolean {

        val current = getPlayableItems().indexOf(getMedia())
        val nextIndex = current + 1

        var next: Media? = null

        if (nextIndex > 0 && nextIndex < getPlayableItems().size) {

            next = getPlayableItems().get(nextIndex)
        }

        return isAutoPlayOn() && next?.autoPlayAsNextReady() == true
    }

    override fun canPrevious(): Boolean {

        return isAutoPlayOn()
    }

    override fun cast(): Boolean {

        // Not supported yet:
        //        val urlToCast = getPlayableItem()?.getStreamUrl()
        //        Console.log("Casting: $urlToCast")

        return false
    }

    override fun share(): Boolean {

        getMedia()?.let {

            return share(it)
        }

        return false
    }

    override fun share(what: Media): Boolean {

        val url = what.getShareUrl()

        url?.let {

            val intent = Intent(Intent.ACTION_SEND)
            intent.setType("text/plain")
            intent.putExtra(Intent.EXTRA_TEXT, url)

            try {

                val noTitle = ""
                val iChooser = Intent.createChooser(intent, noTitle)
                iChooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                BaseApplication.takeContext().startActivity(iChooser)

                return true

            } catch (e: ActivityNotFoundException) {

                recordException(e)
            }
        }

        return false
    }

    override fun next(): Boolean {

        val current = getPlayableItems().indexOf(getMedia())

        getPlayableItems().let {

            if (current < it.lastIndex) {

                getMedia()?.onSkipped()

                return play(it, current + 1, startFrom = 0)
            }
        }

        return false
    }

    override fun hasNext(): Boolean {

        val current = getPlayableItems().indexOf(getMedia())

        getPlayableItems().let {

            if (current < it.lastIndex) {

                return true
            }
        }

        return false
    }

    override fun previous(): Boolean {

        val current = getPlayableItems().indexOf(getMedia())

        if (current > 0) {

            getPlayableItems().let {

                getMedia()?.onSkipped()

                return play(it, current - 1)
            }
        }

        return false
    }

    override fun hasPrevious(): Boolean {

        val current = getPlayableItems().indexOf(getMedia())

        if (current > 0) {

            getPlayableItems().let {

                return true
            }
        }

        return false
    }

    override fun current(): Media? = getMedia()

    override fun getPlayableItems(): List<Media> {

        return getMediaList() ?: emptyList()
    }

    protected abstract fun obtainCurrentProgress(from: Media): Float

    protected abstract fun obtainPlayable(): Pair<Media, Float>?

    protected open fun getPlayable(): Pair<Media, Float>? {

        var currentPlayable: Pair<Media, Float>? = null

        try {

            currentPlayable = obtainPlayable()

        } catch (e: IllegalStateException) {

            recordException(e)
        }

        return currentPlayable
    }

    private fun loadAndPlay(): Boolean {

        val tag = "Load an play ::"
        val currentPlayable = getPlayable()

        Console.log("$tag START: Playable=${currentPlayable != null}")

        currentPlayable?.let { pair ->

            Console.log("$tag Play: ${pair.first.getIdentifier()} @ ${pair.second} sec")

            var started = false
            val playlist = pair.first.getParentPlaylist()

            playlist?.let { p ->

                if (p.isNotEmpty()) {

                    setMediaList(p)

                    val index = p.indexOf(pair.first)
                    started = play(p, index)
                }
            }

            Console.log("$tag Playable items: ${getPlayableItems().size}")

            if (!started) {

                val toSet = if (playlist.isNullOrEmpty()) {

                    listOf(pair.first)

                } else {

                    playlist
                }

                setMediaList(toSet)

                play(pair.first)
            }

            if (started) {

                Console.log("$tag END: Playing")

                return seekTo(pair.second.toInt())
            }
        }

        Console.log("$tag END: No play")

        return false
    }

    private fun instantiateMediaPlayer(): EPlayer? {

        getMediaPlayer()?.let {

            destroyMediaPlayer(it)
        }

        val exoPlayer = ExoPlayer.Builder(BaseApplication.takeContext()).build()

        setMediaPlayer(exoPlayer)

        return exoPlayer
    }

    private fun destroyMediaPlayer(mPlayer: EPlayer? = getMediaPlayer()) {

        if (getPlaying()) {

            try {

                setCurrentDuration(0)
                mPlayer?.stop()

            } catch (e: IllegalStateException) {

                Console.error(e)
            }
        }

        mPlayer?.release()
        clearMediaPlayer()
        setPlaying(false)
    }

    private fun doAfter(code: Int, afterSeconds: Int): Boolean {

        val action = Runnable {

            when (code) {

                BROADCAST_EXTRA_STOP_CODE -> {

                    stop()
                }

                BROADCAST_EXTRA_PLAY_CODE -> {

                    play()
                }

                BROADCAST_EXTRA_PAUSE_CODE -> {

                    pause()
                }
            }
        }

        try {

            exec(action, afterSeconds * 1000L)

            return true

        } catch (e: IllegalStateException) {

            Console.error(e)

        } catch (e: NullPointerException) {

            Console.error(e)
        }

        return false
    }

    override fun invokeCopyRights(): Boolean {

        val current = current()

        current?.let {

            return it.invokeCopyRights()
        }

        return false
    }

    override fun invokeImageGallery(): Boolean {

        val tag = "Invoke image gallery ::"

        Console.log("$tag START")

        val current = current()

        Console.log("$tag Current: $current")

        current?.let {

            Console.log("$tag Invoke: ${it.invokeImageGallery()}")

            return it.invokeImageGallery()
        }

        return false
    }

    private fun startPublishingProgress(mp: EPlayer?) {

        val handler = Handler(Looper.getMainLooper())

        val updateRunnable = object : Runnable {

            override fun run() {

                try {

                    if (getPlaying()) {

                        val currentPosition = mp?.currentPosition ?: 0
                        onProgressChanged(currentPosition, 0)

                        handler.postDelayed(this, 1000)
                    }

                } catch (e: IllegalStateException) {

                    Console.warning(e.message)
                }
            }
        }

        handler.postDelayed(updateRunnable, 1000)
    }

    @Throws(IllegalStateException::class)
    private fun applySpeed(mPlayer: EPlayer? = getMediaPlayer()): Boolean {

        val tag = "SPEED :: APPLY ::"

        Console.log("$tag To: ${getSpeed()}")

        mPlayer?.let { mp ->

            exec(

                onRejected = {

                        err ->
                    recordException(err)
                }

            ) {

                try {

                    // TODO:
//                    var params = mp.playbackParams.setSpeed(getSpeed())
//                    params = params.setSpeed(getSpeed())
//                    mp.playbackParams = params

                    Console.log("$tag APPLIED")

                } catch (e: Exception) {

                    Console.error(e)
                }
            }

            return true
        }

        Console.warning("$tag NOT APPLIED")

        return false
    }

    @Throws(IllegalStateException::class)
    private fun applyVolume(mPlayer: EPlayer? = getMediaPlayer()): Boolean {

        val tag = "VOLUME :: APPLY ::"

        Console.log("$tag To: ${getVolume()}")

        mPlayer?.let { mp ->

            exec(

                onRejected = {

                        err ->
                    recordException(err)
                }

            ) {

                try {

                    // TODO:
//                    val vol = getVolume()
//                    mp.setVolume(vol, vol)

                    Console.log("$tag APPLIED")

                } catch (e: Exception) {

                    Console.error(e)
                }
            }

            return true
        }

        Console.warning("$tag NOT APPLIED")

        return false
    }

    private fun doGetDuration(): Long {

        var mediaDuration = 0L

        getMedia()?.getDuration()?.let {

            if (it > 0) {

                return it
            }
        }

        getMediaPlayer()?.let {

            if (isPrepared()) {

                try {

                    mediaDuration = it.duration / 1000L

                } catch (e: IllegalStateException) {

                    Console.error(e)
                }
            }
        }

        return mediaDuration
    }

    private fun setCurrentDuration(value: Long) {

        val tag = "Current duration :: SET ::"

        Console.log("$tag START :: From = ${getCurrentDuration()}, To = $value")

        onUiThread {

            currentDuration = value
        }

        Console.log("$tag END :: Current = ${getCurrentDuration()}")

    }

    private fun getCurrentDuration(): Long {

        val current: Long = currentDuration

        Console.log("Current duration :: GET :: Current = $current")

        return current
    }

    override fun getMediaPlayer(): EPlayer? {

        return exoPlayer
    }

    override fun setMediaPlayer(value: EPlayer) {

        exoPlayer = value
    }

    override fun unsetMediaPlayer() {

        exoPlayer = null
    }
}