package com.redelf.commons.net.connectivity

import android.content.Context
import android.net.ConnectivityManager

class Connectivity : ConnectivityCheck {

    private val defaultStrategy = object : ConnectivityCheck {

        @Suppress("DEPRECATION")
        override fun isNetworkAvailable(ctx: Context): Boolean {

            val name = Context.CONNECTIVITY_SERVICE

            val connectivityManager = ctx.getSystemService(name) as ConnectivityManager?

            connectivityManager?.let {

                val activeNetworkInfo = it.activeNetworkInfo
                return activeNetworkInfo != null && activeNetworkInfo.isConnected
            }

            return false
        }
    }

    private var checkStrategy: ConnectivityCheck = defaultStrategy

    override fun isNetworkAvailable(ctx: Context) = checkStrategy.isNetworkAvailable(ctx)

    fun setConnectivityCheckStrategy(strategy: ConnectivityCheck) {

        checkStrategy = strategy
    }

    fun resetConnectivityCheckStrategy() {

        checkStrategy = defaultStrategy
    }
}