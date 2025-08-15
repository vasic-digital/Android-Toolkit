package com.redelf.commons.media.player

import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.media.player.base.PlayerConnectivityMonitor
import java.util.concurrent.atomic.AtomicBoolean

class ExoPlayerConnectivityMonitor(

    private val player: ExoPlayer

) : Player.Listener, PlayerConnectivityMonitor {

    private val isPlayerLoading = AtomicBoolean()
    private val context = BaseApplication.takeContext()

    init {

        player.addListener(this)
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {

        isPlayerLoading.set(isLoading)
    }

    override fun hasConnectivity(): Boolean {

        return hasNetwork() && (isPlayerLoading.get() || player.playbackState == Player.STATE_READY)
    }

    private fun hasNetwork(): Boolean {

        val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)

        return caps?.hasCapability(NET_CAPABILITY_INTERNET) == true
    }
}