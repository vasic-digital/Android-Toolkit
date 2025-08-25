package com.redelf.commons.media.player

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.redelf.commons.media.player.base.PlayerConnectivityMonitor
import java.util.concurrent.atomic.AtomicBoolean

class ExoPlayerConnectivityMonitor(

    private val player: ExoPlayer

) : Player.Listener, PlayerConnectivityMonitor {

    private val isPlayerLoading = AtomicBoolean()

    init {

        player.addListener(this)
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {

        isPlayerLoading.set(isLoading)
    }

    override fun hasConnectivity(): Boolean {

        return isPlayerLoading.get() || player.playbackState == Player.STATE_READY
    }
}