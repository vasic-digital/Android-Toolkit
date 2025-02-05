package com.redelf.commons.messaging.firebase

import com.redelf.commons.net.connectivity.ConnectionBlockingBehavior
import com.redelf.commons.net.connectivity.ConnectivityStateChanges
import com.redelf.commons.net.connectivity.StatefulBasicConnectionHandler
import com.redelf.commons.obtain.suspendable.Obtain

class FcmConnectivityHandler private constructor(

    defaultConnectionBlockState: ConnectionBlockingBehavior = ConnectionBlockingBehavior.DO_NOT_BLOCK

) : StatefulBasicConnectionHandler(defaultConnectionBlockState) {

    companion object : Obtain<FcmConnectivityHandler> {

        private var instance: FcmConnectivityHandler? = null

        
        override fun obtain(): FcmConnectivityHandler {

            return obtain(

                ConnectionBlockingBehavior.DO_NOT_BLOCK
            )
        }

        
        fun obtain(

            defaultConnectionBlockState: ConnectionBlockingBehavior

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