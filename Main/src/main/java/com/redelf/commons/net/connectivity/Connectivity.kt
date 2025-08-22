package com.redelf.commons.net.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.os.NetworkOnMainThreadException
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.isOnMainThread
import com.redelf.commons.extensions.recordException
import com.redelf.commons.extensions.yieldWhile
import com.redelf.commons.logging.Console
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException

class Connectivity(

    private val endpoint: String = "",
    private val tag: String = "Connectivity ::"

) : ConnectivityCheck {

    private val defaultStrategy = object : ConnectivityCheck {

        @Suppress("DEPRECATION")
        override fun isNetworkAvailable(ctx: Context): Boolean {

            val tag = "Network connectivity ::"

            if (endpoint.isEmpty()) {

                val name = Context.CONNECTIVITY_SERVICE

                val connectivityManager = ctx.getSystemService(name) as ConnectivityManager?

                connectivityManager?.let {

                    val activeNetworkInfo = it.activeNetworkInfo
                    return activeNetworkInfo != null && activeNetworkInfo.isConnected
                }

                return false
            }

            try {

                if (isOnMainThread()) {

                    val e = NetworkOnMainThreadException()
                    recordException(e)
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

        override fun requireNetworkAvailable(ctx: Context): Boolean {

            fun notConnected(): Boolean {

                return !isNetworkAvailable(ctx)
            }

            if (notConnected()) {

                Console.warning(

                    "$tag NO INTERNET CONNECTION :: Waiting for it".trim()
                )

                yieldWhile(

                    timeoutInMilliseconds = 60 * 1000L

                ) {

                    notConnected()
                }

                if (notConnected()) {

                    val msg = "$tag NO INTERNET CONNECTION :: Waiting timeout"
                    val e = IOException(msg)
                    recordException(e)
                }
            }

            return isNetworkAvailable(ctx)
        }
    }

    private var checkStrategy: ConnectivityCheck = defaultStrategy

    override fun isNetworkAvailable(ctx: Context) = checkStrategy.isNetworkAvailable(ctx)

    override fun requireNetworkAvailable(ctx: Context) = checkStrategy.requireNetworkAvailable(ctx)

    fun isNetworkUnavailable(ctx: Context) = !isNetworkAvailable(ctx)

    fun setConnectivityCheckStrategy(strategy: ConnectivityCheck) {

        checkStrategy = strategy
    }

    fun resetConnectivityCheckStrategy() {

        checkStrategy = defaultStrategy
    }
}