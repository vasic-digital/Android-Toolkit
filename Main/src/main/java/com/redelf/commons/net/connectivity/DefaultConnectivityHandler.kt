package com.redelf.commons.net.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.execution.Retrying
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.ObtainParametrized
import java.util.concurrent.Callable

class DefaultConnectivityHandler private constructor(

    ctx: Context,
    defaultConnectionBlockState: ConnectionBlockingBehavior = ConnectionBlockingBehavior.DO_NOT_BLOCK

) : StatefulBasicConnectionHandler(defaultConnectionBlockState) {

    companion object : ObtainParametrized<DefaultConnectivityHandler, Context> {

        private var instance: DefaultConnectivityHandler? = null

        
        override fun obtain(param: Context): DefaultConnectivityHandler {

            return obtain(

                param,
                ConnectionBlockingBehavior.DO_NOT_BLOCK
            )
        }

        
        fun obtain(

            param: Context,
            defaultConnectionBlockState: ConnectionBlockingBehavior

        ): DefaultConnectivityHandler {

            instance?.let {

                return it
            }

            val handler = DefaultConnectivityHandler(param, defaultConnectionBlockState)
            instance = handler
            return handler
        }
    }

    private val networkCallbacks = Callbacks<ConnectivityStateChanges>("Network")

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        private val tag = "NETWORK CALLBACK ::"

        override fun onAvailable(network: Network) {

            Console.log("$tag On network available")

            notifyNetworkCallbacks()
        }

        override fun onLost(network: Network) {

            Console.warning("$tag On network lost")

            notifyNetworkCallbacks()
        }
    }

    init {

        val retrying = Retrying(10)
        val appCtx = ctx.applicationContext
        val systemService = Context.CONNECTIVITY_SERVICE
        val connectivityManager = appCtx.getSystemService(systemService) as ConnectivityManager?

        val callable = object : Callable<Boolean> {

            override fun call(): Boolean {

                try {

                    connectivityManager?.registerDefaultNetworkCallback(networkCallback)

                    return true

                } catch (e: Exception) {

                    Console.error(e)
                }

                return false
            }
        }

        val times = retrying.execute(callable::call)

        if (times > 0) {

            Console.warning("Network callback registered after $times times")
        }
    }

    override fun register(subscriber: ConnectivityStateChanges) {

        networkCallbacks.register(subscriber)
    }

    override fun unregister(subscriber: ConnectivityStateChanges) {

        networkCallbacks.unregister(subscriber)
    }

    override fun isRegistered(subscriber: ConnectivityStateChanges): Boolean {

        return networkCallbacks.isRegistered(subscriber)
    }

    private fun notifyNetworkCallbacks() {

        networkCallbacks.doOnAll(

            object : CallbackOperation<ConnectivityStateChanges> {

                override fun perform(callback: ConnectivityStateChanges) {

                    callback.onStateChanged()
                }
            },

            operationName = "Network_State_Changed"
        )
    }
}