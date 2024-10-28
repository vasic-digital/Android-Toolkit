package com.redelf.commons.connectivity.indicator.implementation

import com.redelf.commons.application.BaseApplication
import com.redelf.commons.connectivity.indicator.connection.ConnectionAvailableService
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulService
import com.redelf.commons.creation.instantiation.SingleInstance
import com.redelf.commons.net.connectivity.ConnectionState
import com.redelf.commons.net.connectivity.DefaultConnectivityHandler
import com.redelf.commons.net.connectivity.StatefulBasicConnectionHandler
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.obtain.Obtainer

class InternetConnectionAvailabilityService private constructor(caller: String) :

    ConnectionAvailabilityService(

        handlerObtain = object : Obtain<StatefulBasicConnectionHandler> {

            override fun obtain(): StatefulBasicConnectionHandler {

                val ctx = BaseApplication.takeContext()

                return DefaultConnectivityHandler.obtain(ctx)
            }
        },

        caller = caller

    )
{

    companion object :

        SingleInstance<ConnectionAvailableService>(),
        Obtainer<AvailableStatefulService>

    {

        @Throws(IllegalArgumentException::class)
        override fun instantiate(vararg params: Any): ConnectionAvailableService {

            if (params.isEmpty() || params[0] !is String) {

                throw IllegalArgumentException("Caller parameter must be a String")
            }

            return InternetConnectionAvailabilityService(params[0] as String)
        }

        override fun getObtainer(): Obtain<AvailableStatefulService> {

            return object : Obtain<AvailableStatefulService> {

                override fun obtain() = instantiate()
            }
        }
    }

    override fun getState(): ConnectionState {

        cHandler?.let {

            if (it.isNetworkAvailable(takeContext())) {

                return ConnectionState.Connected
            }
        }

        return ConnectionState.Disconnected
    }

    override fun getWho() = "Internet connection"

    override val tag: String = "${identifier()} ::"

    override fun identifier() = "Internet connection availability"
}