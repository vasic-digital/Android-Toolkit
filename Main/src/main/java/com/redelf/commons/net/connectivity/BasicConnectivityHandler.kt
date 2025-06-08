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
                "Hash code = '${hashCode()}' ::"

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