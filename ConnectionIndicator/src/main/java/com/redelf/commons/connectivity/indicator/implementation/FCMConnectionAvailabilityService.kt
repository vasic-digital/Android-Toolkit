package com.redelf.commons.connectivity.indicator.implementation

import com.redelf.commons.connectivity.indicator.connection.ConnectionAvailableService
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulService
import com.redelf.commons.creation.instantiation.SingleInstance
import com.redelf.commons.creation.instantiation.SingleInstantiated
import com.redelf.commons.logging.Console
import com.redelf.commons.messaging.firebase.FcmConnectivityHandler
import com.redelf.commons.messaging.firebase.FcmService
import com.redelf.commons.net.connectivity.Reconnect
import com.redelf.commons.net.connectivity.StatefulBasicConnectionHandler
import com.redelf.commons.obtain.Obtainer
import com.redelf.commons.obtain.suspendable.Obtain
import kotlin.jvm.Throws

class FCMConnectionAvailabilityService private constructor(origin: String) :

    ConnectionAvailabilityService(

        handlerObtain = object : Obtain<StatefulBasicConnectionHandler> {

            override fun obtain(): StatefulBasicConnectionHandler {

                return FcmConnectivityHandler.obtain()
            }
        },

        origin = origin,

    ), Reconnect, SingleInstantiated
{

    companion object :

        SingleInstance<ConnectionAvailableService>(),
        Obtainer<AvailableStatefulService>

    {

        @Throws(IllegalArgumentException::class)
        override fun instantiate(vararg params: Any): ConnectionAvailableService {

            if (params.isEmpty() || params[0] !is String) {

                throw IllegalArgumentException("Origin parameter must be a String")
            }

            return FCMConnectionAvailabilityService(params[0] as String)
        }

        @Throws(IllegalArgumentException::class)
        override fun getObtainer(vararg params: Any): Obtain<AvailableStatefulService> {

            if (params.isEmpty() || params[0] !is String) {

                throw IllegalArgumentException("Origin parameter must be a String")
            }

            return object : Obtain<AvailableStatefulService> {

                override fun obtain() = instantiate(params[0] as String)
            }
        }
    }

    override fun getWho() = "Push notifications"

    override fun identifier() = "Connectivity :: Availability :: FCM :: ${hashCode()}"

    override fun reconnect() {

        Console.log("${tag()} Reconnecting...")

        FcmService.reconnect()
    }
}