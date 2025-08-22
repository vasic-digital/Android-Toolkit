package com.redelf.commons.net.connectivity

import android.content.Context
import android.os.NetworkOnMainThreadException
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.isOnMainThread
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import java.net.InetAddress
import java.net.UnknownHostException

class Connectivity(private val endpoint: String = CHECK_ENDPOINT) : ConnectivityCheck {

    companion object {

        var CHECK_ENDPOINT = "www.google.com"
    }

    private val defaultStrategy = object : ConnectivityCheck {

        @Suppress("DEPRECATION")
        override fun isNetworkAvailable(ctx: Context): Boolean {

            val tag = "Network connectivity ::"

            try {

                if (isOnMainThread()) {

                    Console.error("$tag Network check on main thread not allowed")
                    val e = NetworkOnMainThreadException()
                    recordException(e)

                    return false
                }

                val address = InetAddress.getByName(endpoint)
                val online = isNotEmpty(address.toString())

                if (online) {

                    Console.log("$tag Online")

                } else {

                    Console.warning("$tag Offline")
                }

                return online

            } catch (e: UnknownHostException) {

                Console.warning("$tag Offline :: Error (1) = '${e.message}'")

                return false

            } catch (e: Throwable) {

                Console.warning("$tag Offline :: Error (2) = '${e.message}'")

                return false
            }
        }
    }

    private var checkStrategy: ConnectivityCheck = defaultStrategy

    override fun isNetworkAvailable(ctx: Context) = checkStrategy.isNetworkAvailable(ctx)

    fun isNetworkUnavailable(ctx: Context) = !isNetworkAvailable(ctx)

    fun setConnectivityCheckStrategy(strategy: ConnectivityCheck) {

        checkStrategy = strategy
    }

    fun resetConnectivityCheckStrategy() {

        checkStrategy = defaultStrategy
    }
}