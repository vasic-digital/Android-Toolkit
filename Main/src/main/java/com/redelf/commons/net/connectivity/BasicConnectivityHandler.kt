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

        Console.log(

            "Connectivity :: Handler :: ${this.javaClass.simpleName} ${hashCode()} " +
                    ":: isNetworkAvailable"
        )

        if (blockConnection.get()) {

            return false
        }

        return Connectivity().isNetworkAvailable(ctx)
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