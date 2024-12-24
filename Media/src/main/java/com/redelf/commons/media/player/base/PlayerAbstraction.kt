package com.redelf.commons.media.player.base

import com.redelf.commons.creation.instantiation.Instantiable
import com.redelf.commons.execution.ExecuteWithResult
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.onUiThread
import com.redelf.commons.logging.Console
import com.redelf.commons.media.Media
import com.redelf.commons.obtain.OnObtain
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

    abstract fun getMediaPlayer(): MP?

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

    protected abstract fun setMediaPlayer(value: MP)

    protected abstract fun unsetMediaPlayer()

    protected fun clearMediaPlayer() {

        prepared.set(false)

        unsetMediaPlayer()
    }

    protected open fun clearMedia() {

        media = null
    }

    protected open fun getPlaying() = playing.get()

    protected open fun setPlaying(value: Boolean) = playing.set(value)

    protected fun isPrepared() = prepared.get()

    protected fun setPrepared() = prepared.set(true)

    protected fun withPlayer(

        operation: String = "",
        onUiThread: Boolean = true,
        callback: OnObtain<MP?>? = null,
        instantiation: Instantiable<MP>? = null,

        doWhat: (ep: MP) -> Boolean

    ) {

        val player = getMediaPlayer()
        val tag = "Player :: Operation = $operation ::"

        Console.log("$tag START")

        if (player == null && instantiation == null) {

            val e = IllegalStateException("Player is not available")
            callback?.onFailure(e)
            return
        }

        val action = Runnable {

            val instance = if (instantiation != null) {

                instantiation.instantiate()

            } else {

                player
            }

            instance?.let {

                Console.log("$tag RUN :: START")

                if (doWhat(it)) {

                    Console.log("$tag RUN :: END")
                    callback?.onCompleted(it)

                } else {

                    val msg = "Failed to execute the player operation"
                    val e = IllegalArgumentException("$msg $operation".trim())

                    Console.error("$tag RUN :: ERROR: $msg")
                    callback?.onFailure(e)
                }
            }

            if (instance == null) {

                val e = IllegalStateException("Player is not available")
                callback?.onFailure(e)
            }
        }

        if (onUiThread) {

            onUiThread {

                Console.log("$tag UI :: PRE-RUN")

                action.run()

                Console.log("$tag UI :: POST-RUN")
            }

        } else {

            Console.log("$tag PRE-RUN")

            exec(

                onRejected = {

                    callback?.onFailure(it)
                }

            ) {

                action.run()
            }

            Console.log("$tag POST-RUN")
        }
    }
}