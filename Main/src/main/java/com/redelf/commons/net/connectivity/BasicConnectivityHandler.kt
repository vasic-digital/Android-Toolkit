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
import com.redelf.commons.logging.Console
import java.util.concurrent.atomic.AtomicBoolean

open class BasicConnectivityHandler(

    defaultConnectionBlockState: ConnectionBlockingBehavior =
        ConnectionBlockingBehavior.DO_NOT_BLOCK

) : ConnectivityHandler {

    private val blockConnection = AtomicBoolean(defaultConnectionBlockState.value)

    override fun isNetworkAvailable(ctx: Context): Boolean {

        val tag = "Network connectivity :: Handler :: ${this.javaClass.simpleName} :: " +
                "Hash  = '${hashCode()}' ::"

        Console.log("$tag START")

        if (blockConnection.get()) {

            Console.warning("$tag Offline due to blocking state")

            return false
        }

        Console.log("$tag Checking")

        val online = Connectivity().isNetworkAvailable(ctx)

        Console.log("$tag Checked")

        if (online) {

            Console.log("$tag Online")

        } else {

            Console.warning("$tag Offline")
        }

        return online
    }

    override fun requireNetworkAvailable(ctx: Context): Boolean {

        if (blockConnection.get()) {

            return false
        }

        return Connectivity().requireNetworkAvailable(ctx)
    }

    override fun toggleConnection() {

        blockConnection.set(blockConnection.get())
    }

    override fun connectionOff() {

        blockConnection.set(true)
    }

    override fun connectionOn() {

        blockConnection.set(false)
    }
}