package com.redelf.commons.connectivity.indicator.implementation

import com.redelf.commons.connectivity.indicator.connection.ConnectionAvailableService
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulService
import com.redelf.commons.creation.instantiation.SingleInstance
import com.redelf.commons.logging.Console
import com.redelf.commons.messaging.firebase.FcmConnectivityHandler
import com.redelf.commons.net.connectivity.Reconnectable
import com.redelf.commons.net.connectivity.StatefulBasicConnectionHandler
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.obtain.Obtainer

class FCMConnectionAvailabilityService private constructor() :

    ConnectionAvailabilityService(

        handlerObtain = object : Obtain<StatefulBasicConnectionHandler> {

            override fun obtain(): StatefulBasicConnectionHandler {

                return FcmConnectivityHandler.obtain()
            }
        }

    ), Reconnectable
{

    companion object :

        SingleInstance<ConnectionAvailableService>(),
        Obtainer<AvailableStatefulService>

    {

        override fun instantiate(): ConnectionAvailableService {

            return FCMConnectionAvailabilityService()
        }

        override fun getObtainer(): Obtain<AvailableStatefulService> {

            return object : Obtain<AvailableStatefulService> {

                override fun obtain() = instantiate()
            }
        }
    }

    override fun getWho() = "Push notifications"

    override val tag: String = "${identifier()} ::"

    override fun identifier() = "FCM connection availability"

    override fun reconnect() {

        Console.log("$tag Reconnecting...")

        // TODO: Implement
    }
}