package com.redelf.commons.messaging.firebase

import android.content.Context
import com.redelf.commons.net.connectivity.ConnectionBlockingBehavior
import com.redelf.commons.net.connectivity.ConnectivityStateChanges
import com.redelf.commons.net.connectivity.StatefulBasicConnectionHandler
import com.redelf.commons.obtain.ObtainParametrized

class FcmConnectivityHandler private constructor(

    defaultConnectionBlockState: ConnectionBlockingBehavior = ConnectionBlockingBehavior.DO_NOT_BLOCK

) : StatefulBasicConnectionHandler(defaultConnectionBlockState)

{

    companion object : ObtainParametrized<FcmConnectivityHandler, Context> {

        private var instance: FcmConnectivityHandler? = null

        override fun obtain(param: Context): FcmConnectivityHandler {

            return obtain(param)
        }

        fun obtain(

            defaultConnectionBlockState: ConnectionBlockingBehavior =
                ConnectionBlockingBehavior.DO_NOT_BLOCK

        ): FcmConnectivityHandler {

            instance?.let {

                return it
            }

            val handler = FcmConnectivityHandler(defaultConnectionBlockState)
            instance = handler
            return handler
        }
    }

    override fun register(subscriber: ConnectivityStateChanges) {

        FcmService.register(subscriber)
    }

    override fun isRegistered(subscriber: ConnectivityStateChanges): Boolean {

        return FcmService.isRegistered(subscriber)
    }

    override fun unregister(subscriber: ConnectivityStateChanges) {

        FcmService.unregister(subscriber)
    }
}