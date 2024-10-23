package com.redelf.commons.messaging.firebase

import android.content.Context
import com.redelf.commons.net.connectivity.BasicConnectivityHandler
import com.redelf.commons.net.connectivity.ConnectionBlockingBehavior
import com.redelf.commons.net.connectivity.ConnectivityStateChanges
import com.redelf.commons.registration.Registration

class FcmConnectivityHandler(

    ctx: Context,
    defaultConnectionBlockState: ConnectionBlockingBehavior = ConnectionBlockingBehavior.DO_NOT_BLOCK

) : BasicConnectivityHandler(defaultConnectionBlockState),
    Registration<ConnectivityStateChanges>

{

    // TODO: Implement - #Availability
    override fun register(subscriber: ConnectivityStateChanges) {

        TODO("Not yet implemented")
    }

    override fun isRegistered(subscriber: ConnectivityStateChanges): Boolean {

        TODO("Not yet implemented")
    }

    override fun unregister(subscriber: ConnectivityStateChanges) {

        TODO("Not yet implemented")
    }
}