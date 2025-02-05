package com.redelf.commons.media.player

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.creation.instantiation.Instantiable
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.onUiThread
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.media.Media
import com.redelf.commons.media.player.base.PlayerAbstraction
import com.redelf.commons.obtain.Obtain
import java.util.UUID
import kotlin.collections.indexOf

typealias EPlayer = ExoPlayer

abstract class ExoPlayer : PlayerAbstraction<EPlayer>() {

    val playerTag = "Player :: Exo ::"

    companion object {

        private var exo: EPlayer? = null
    }

    private var currentDuration: Long = 0
    private var updateRunnable: MediaRunnable? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun reset() {

        clearMedia()
    }

    override fun isAutoPlayOff(): Boolean {

        return !isAutoPlayOn()
    }

    override fun playAsync() {

        Console.log("$playerTag Play async")

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

        Console.log(

            "$playerTag $logTag ${what.size} :: $index :: " +
                    "$startFrom :: ${what[index].getIdentifier()}"
        )

        if (isPlaying()) {

            withPlayer(operation = "play") {

                destroyMediaPlayer(it)
            }
        }

        setMediaList(what)
        setMedia(what[index])

        Console.log("$playerTag $logTag Set: ${getMedia()?.getIdentifier()}")

        getMedia()?.let {

            Console.log("$playerTag $logTag Execute: ${it.getIdentifier()}")

            return execute(it, startFrom)
        }

        Console.error("$playerTag $logTag No playable item")

        return true
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

            withPlayer(operation = "assign") {

                destroyMediaPlayer(it)
            }
        }

        setMediaList(what)
        setMedia(what[index])

        return true
    }

    private fun execute(what: Media, startFrom: Int): Boolean {

        val logTag = "Player :: Play :: Execution :: ${what.getIdentifier()} ::"

        Console.log("$playerTag $logTag Start :: From = $startFrom")

        exec {

            val streamUrl = what.getStreamUrl()

            withPlayer(

                operation = "execute",

                instantiation = object : Instantiable<EPlayer> {

                    override fun instantiate(vararg params: Any): EPlayer {

                        return instantiateMediaPlayer()
                    }
                }

            ) { ep ->

                Console.log("$playerTag $logTag Player instantiated")

                try {

                    if (getPlaying()) {

                        doStop(ep)
                    }

                    if (isEmpty(streamUrl)) {

                        Console.error("$playerTag $logTag Empty stream url")

                        false
                    }

                    val stateListener = object : Player.Listener {

                        private val tag = "$playerTag $logTag State listener ::"

                        override fun onPlaybackStateChanged(state: Int) {

                            when (state) {

                                Player.STATE_READY -> {

                                    setPrepared()

                                    Console.debug("$tag Prepared")

                                    setPlaying(true)

                                    what.onStarted()
                                }

                                Player.STATE_ENDED -> {

                                    withPlayer(operation = "on ended") {

                                        doStop(it)

                                        Console.debug("$tag Ended")

                                        what.onEnded()

                                        if (canNext()) {

                                            next()
                                        }

                                        true
                                    }
                                }

                                Player.STATE_BUFFERING -> {

                                    Console.debug("$tag Buffering")
                                }

                                Player.STATE_IDLE -> {

                                    Console.debug("$tag Idle")
                                }

                                else -> {

                                    Console.debug("$tag Unknown")
                                }
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {

                            val msg =
                                "ExoPlayer error: ${error.errorCode}, extra: ${error.errorCodeName}"

                            val e = IllegalStateException(msg)

                            what.onError(e)
                            Console.error("$playerTag $logTag Error: ${error.errorCode}")
                        }
                    }

                    ep.addListener(stateListener)

                    streamUrl?.let {

                        Console.log("$playerTag $logTag Stream url: $streamUrl")

                        applySpeed(ep)
                        setVolume(1.0f)

                        val mediaItem = MediaItem.fromUri(streamUrl)
                        ep.setMediaItem(mediaItem)

                        val duration = doGetDuration()

                        setCurrentDuration(duration)

                        Console.log("$playerTag $logTag START :: Duration = $duration")

                        val currentProgress: Float = if (startFrom < 0) {

                            obtainCurrentProgress(what)

                        } else {

                            startFrom.toFloat()
                        }

                        currentProgress.let { progress ->

                            Console.log("$playerTag $logTag Progress obtained: $currentProgress")

                            if (currentProgress >= duration * 0.95) {

                                seekTo(0)

                            } else {

                                seekTo(progress.toInt())
                            }

                            Console.log("$playerTag $logTag Seek")
                        }

                        Console.log(

                            "$playerTag $logTag Preparing :: Player hash = ${ep.hashCode()}"
                        )

                        ep.prepare()
                        ep.playWhenReady = true

                        startPublishingProgress()

                        Console.log("$playerTag $logTag On started")
                    }

                } catch (e: Exception) {

                    Console.error("$playerTag $logTag ${e::class.simpleName} :: ${e.message}")
                    recordException(e)
                    false
                }

                true
            }
        }

        return true
    }

    override fun execute(what: Media): Boolean {

        return execute(what, -1)
    }

    override fun stop() {

        Console.log("$playerTag stop()")

        withPlayer(

            operation = "stop"

        ) {

            doStop(it)
        }
    }

    override fun stop(afterSeconds: Int) = doAfter(BROADCAST_EXTRA_STOP_CODE, afterSeconds)

    override fun play(afterSeconds: Int) = doAfter(BROADCAST_EXTRA_PLAY_CODE, afterSeconds)

    override fun pause(afterSeconds: Int) = doAfter(BROADCAST_EXTRA_PAUSE_CODE, afterSeconds)

    override fun pause() {

        if (getPlaying()) {

            withPlayer(operation = "pause") {

                try {

                    it.pause()
                    setPlaying(false)
                    getMedia()?.onPaused()
                    stopPublishingProgress()

                } catch (e: IllegalStateException) {

                    Console.error("$playerTag ERROR: ${e.message}")
                    false
                }

                true
            }
        }
    }

    override fun resume(): Boolean {

        if (!getPlaying()) {

            withPlayer(operation = "resume") {

                try {

                    it.prepare()
                    it.playWhenReady = true

                    setPlaying(true)
                    getMedia()?.onResumed()
                    startPublishingProgress()

                } catch (e: IllegalStateException) {

                    Console.warning(e.message)

                    false
                }

                true
            }
        }

        return true
    }

    override fun seekTo(positionInMilliseconds: Float): Boolean {

        Console.log("$playerTag Seek to: $positionInMilliseconds milliseconds")

        withPlayer(operation = "seek to position $positionInMilliseconds ms") {

            try {

                Console.log("$playerTag Seek to: $positionInMilliseconds")

                it.seekTo(positionInMilliseconds.toLong())

                Console.log("$playerTag Seek to: $positionInMilliseconds done")

                true

            } catch (e: Exception) {

                Console.log("$playerTag ERROR: ${e.message}")

                false
            }
        }

        return true
    }

    override fun seekTo(positionInSeconds: Int): Boolean {

        Console.log("$playerTag Seek to: $positionInSeconds seconds")

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

                    return it.currentPosition.toInt()

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

        Console.log("$playerTag $tag To: ${getSpeed()}")

        withPlayer(operation = "set speed") {

            applySpeed(it)
        }

        return true
    }

    override fun setVolume(value: Float): Boolean {

        setVolumeValue(value)

        val tag = "VOLUME :: SET ::"

        Console.log("$playerTag $tag To: ${getVolume()}")

        return applyVolume()
    }

    override fun resetSpeed(): Boolean {

        val tag = "SPEED :: RESET ::"

        val toSpeed = 1f
        val reset = setSpeed(toSpeed)

        if (reset) {

            Console.log("$playerTag $tag To: ${getSpeed()}")

        } else {

            Console.warning("$playerTag $tag Failed")
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
        //        Console.log("$playerTag Casting: $urlToCast")

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
                iChooser.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
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

            Console.error("$playerTag Obtain playable failed :: ERROR: ${e.message}")
            recordException(e)
        }

        return currentPlayable
    }

    private fun loadAndPlay(): Boolean {

        val tag = "Load an play ::"
        val currentPlayable = getPlayable()

        Console.log("$playerTag $tag START: Playable = ${currentPlayable != null}")

        currentPlayable?.let { pair ->

            Console.log("$playerTag $tag Play: ${pair.first.getIdentifier()} @ ${pair.second} sec")

            var started = false
            val playlist = pair.first.getParentPlaylist()

            playlist?.let { p ->

                if (p.isNotEmpty()) {

                    setMediaList(p)

                    val index = p.indexOf(pair.first)
                    started = play(p, index)
                }
            }

            Console.log("$playerTag $tag Playable items: ${getPlayableItems().size}")

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

                Console.log("$playerTag $tag END: Playing")

                return seekTo(pair.second.toInt())
            }
        }

        Console.log("$playerTag $tag END: No play")

        return false
    }

    @OptIn(UnstableApi::class)
    private fun instantiateMediaPlayer(): EPlayer {

        val player = getMediaPlayer()

        player?.let {

            destroyMediaPlayer(it)
        }

        val context = BaseApplication.takeContext()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(

                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS * 2,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(30000)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(mapOf("User-Agent" to "ExoPlayer"))

        val exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .build()

        exoPlayer.addAnalyticsListener(object : AnalyticsListener {

            override fun onAudioInputFormatChanged(

                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?

            ) {

                super.onAudioInputFormatChanged(eventTime, format, decoderReuseEvaluation)

                Console.log(

                    "$playerTag Audio format changed: ${format.sampleMimeType}, " +
                            "Codec: ${format.codecs}"
                )
            }
        })

        setMediaPlayer(exoPlayer)

        return exoPlayer
    }

    private fun destroyMediaPlayer(player: EPlayer): Boolean {

        var success = true

        if (getPlaying()) {

            try {

                setCurrentDuration(0)
                player.stop()

            } catch (e: Exception) {

                success = false
                recordException(e)
                Console.error("$playerTag Stop failed :: ${e.message}")
            }
        }

        try {

            player.release()
            clearMediaPlayer()
            setPlaying(false)

        } catch (e: Exception) {

            success = false
            recordException(e)
            Console.error("$playerTag Release failed :: ${e.message}")
        }

        return success
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

        Console.log("$playerTag $tag START")

        val current = current()

        Console.log("$playerTag $tag Current: $current")

        current?.let {

            Console.log("$playerTag $tag Invoke: ${it.invokeImageGallery()}")

            return it.invokeImageGallery()
        }

        return false
    }

    override fun clearMedia() {
        super.clearMedia()

        stopPublishingProgress()
    }

    private fun stopPublishingProgress() {

        updateRunnable?.let {

            try {

                handler.removeCallbacks(it)

            } catch (e: Exception) {

                recordException(e)
            }
        }

        updateRunnable = null
    }

    private fun startPublishingProgress(frequency: Long = 1000L) {

        updateRunnable?.let {

            if (it.identifier == getMedia()?.getIdentifier()) {

                return
            }
        }

        stopPublishingProgress()

        updateRunnable = MediaRunnable(

            identifier = getMedia()?.getIdentifier() ?: UUID.randomUUID(),

            getCurrentIdentifier = object : Obtain<UUID?> {

                override fun obtain(): UUID? {

                    return getMedia()?.getIdentifier()
                }
            },

            getCurrentPosition = object : Obtain<Long> {

                override fun obtain(): Long {

                    return getCurrentPosition().toLong()
                }
            },

            handler = handler,
            frequency = frequency

        ) { position, _ ->

            onProgressChanged(position, 0)
        }

        updateRunnable?.let {

            handler.postDelayed(it, frequency)
        }
    }

    @Throws(IllegalStateException::class)
    private fun applySpeed(ep: EPlayer): Boolean {

        val tag = "SPEED :: APPLY ::"

        Console.log("$playerTag $tag To: ${getSpeed()}")

        try {

            val playbackParameters = PlaybackParameters(getSpeed())
            ep.playbackParameters = playbackParameters

            Console.log("$playerTag $tag APPLIED")

            return true

        } catch (e: Exception) {

            Console.error("$playerTag $tag NOT APPLIED")
            Console.error(e)
        }

        return false
    }

    @Throws(IllegalStateException::class)
    private fun applyVolume(): Boolean {

        val tag = "VOLUME :: APPLY ::"

        Console.log("$playerTag $tag To: ${getVolume()}")

        if (isPlaying()) {

            withPlayer(operation = "apply volume") {

                try {

                    val vol = getVolume()
                    it.volume = vol

                    Console.log("$playerTag $tag APPLIED")

                    true

                } catch (e: Exception) {

                    Console.error("$playerTag $tag NOT APPLIED")
                    Console.error(e)
                }

                false
            }
        }

        return true
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

        Console.log("$playerTag $tag START :: From = ${getCurrentDuration()}, To = $value")

        onUiThread {

            currentDuration = value
        }

        Console.log("$playerTag $tag END :: Current = ${getCurrentDuration()}")
    }

    private fun getCurrentDuration() = currentDuration

    override fun getMediaPlayer(): EPlayer? {

        return exo
    }

    override fun setMediaPlayer(value: EPlayer) {

        Console.log("$playerTag Set player :: ${value.hashCode()}")

        exo = value
    }

    override fun unsetMediaPlayer() {

        Console.log("$playerTag UnSet player")

        exo = null
    }

    private fun doStop(ep: EPlayer): Boolean {

        val success = destroyMediaPlayer(ep)

        setPlaying(false)
        getMedia()?.onStopped()
        stopPublishingProgress()

        return success
    }

    private class MediaRunnable(

        val identifier: UUID,

        private val handler: Handler,
        private val frequency: Long,
        private val getCurrentIdentifier: Obtain<UUID?>,
        private val getCurrentPosition: Obtain<Long>,
        private val callback: (Long, Int) -> Unit

    ) : Runnable {

        override fun run() {

            if (identifier == getCurrentIdentifier.obtain()) {

                val currentPosition = getCurrentPosition.obtain()

                try {

                    callback(currentPosition, 0)

                    handler.postDelayed(this, frequency)

                } catch (e: IllegalStateException) {

                    Console.warning(e.message)
                }
            }
        }
    }
}