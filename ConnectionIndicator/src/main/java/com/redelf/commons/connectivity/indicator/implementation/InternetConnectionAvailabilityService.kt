package com.redelf.commons.connectivity.indicator.implementation

import com.redelf.commons.application.BaseApplication
import com.redelf.commons.connectivity.indicator.connection.ConnectionAvailableService
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulService
import com.redelf.commons.creation.instantiation.SingleInstance
import com.redelf.commons.creation.instantiation.SingleInstantiated
import com.redelf.commons.net.connectivity.DefaultConnectivityHandler
import com.redelf.commons.net.connectivity.StatefulBasicConnectionHandler
import com.redelf.commons.obtain.Obtainer
import com.redelf.commons.obtain.suspendable.Obtain

class InternetConnectionAvailabilityService private constructor(origin: String) :

    ConnectionAvailabilityService(

        handlerObtain = object : Obtain<StatefulBasicConnectionHandler> {

            override fun obtain(): StatefulBasicConnectionHandler {

                val ctx = BaseApplication.takeContext()

                return DefaultConnectivityHandler.obtain(ctx)
            }
        },

        origin = origin

    ), SingleInstantiated
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

            return InternetConnectionAvailabilityService(params[0] as String)
        }

        override fun getObtainer(vararg params: Any): Obtain<AvailableStatefulService> {

            if (params.isEmpty() || params[0] !is String) {

                throw IllegalArgumentException("Origin parameter must be a String")
            }

            return object : Obtain<AvailableStatefulService> {

                override fun obtain() = instantiate(params[0] as String)
            }
        }
    }

    override fun getWho() = "Internet connection"

    override fun identifier() = "Connectivity :: Availability :: Internet :: ${hashCode()}"
}