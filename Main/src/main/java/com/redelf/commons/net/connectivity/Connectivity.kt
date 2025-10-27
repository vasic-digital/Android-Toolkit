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


package com.redelf.commons.net.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.os.NetworkOnMainThreadException
import com.redelf.commons.application.BaseApplication
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
    private val alwaysRequire: Boolean = false,
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

                    if (BaseApplication.takeContext().isProduction()) {

                        recordException(e)

                    } else {

                        Console.error(e.message ?: e::class.java.simpleName)
                    }
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

                } else {

                    Console.warning(

                        ("$tag NO INTERNET CONNECTION :: " +
                                "Connection has been recovered with SUCCESS").trim()
                    )
                }
            }

            return isNetworkAvailable(ctx)
        }
    }

    private var checkStrategy: ConnectivityCheck = defaultStrategy

    override fun requireNetworkAvailable(ctx: Context) = checkStrategy.requireNetworkAvailable(ctx)

    override fun isNetworkAvailable(ctx: Context): Boolean {

        if (alwaysRequire) {

            return requireNetworkAvailable(ctx)
        }

        return checkStrategy.isNetworkAvailable(ctx)
    }

    fun isNetworkUnavailable(ctx: Context) = !isNetworkAvailable(ctx)

    fun setConnectivityCheckStrategy(strategy: ConnectivityCheck) {

        checkStrategy = strategy
    }

    fun resetConnectivityCheckStrategy() {

        checkStrategy = defaultStrategy
    }
}