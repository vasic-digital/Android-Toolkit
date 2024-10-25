package com.redelf.commons.messaging.firebase

import android.content.Context
import com.redelf.commons.net.connectivity.BasicConnectivityHandler
import com.redelf.commons.net.connectivity.ConnectionBlockingBehavior
import com.redelf.commons.net.connectivity.ConnectivityStateChanges
import com.redelf.commons.obtain.ObtainParametrized
import com.redelf.commons.registration.Registration

class FcmConnectivityHandler private constructor(

    defaultConnectionBlockState: ConnectionBlockingBehavior = ConnectionBlockingBehavior.DO_NOT_BLOCK

) :

    BasicConnectivityHandler(defaultConnectionBlockState),
    Registration<ConnectivityStateChanges>

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