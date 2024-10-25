package com.redelf.commons.connectivity.indicator.implementation

import com.redelf.commons.application.BaseApplication
import com.redelf.commons.connectivity.indicator.connection.ConnectionAvailableService
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulService
import com.redelf.commons.creation.instantiation.SingleInstance
import com.redelf.commons.net.connectivity.DefaultConnectivityHandler
import com.redelf.commons.net.connectivity.StatefulBasicConnectionHandler
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.obtain.Obtainer

class InternetConnectionAvailabilityService private constructor() :

    ConnectionAvailabilityService(

        handlerObtain = object : Obtain<StatefulBasicConnectionHandler> {

            override fun obtain(): StatefulBasicConnectionHandler {

                val ctx = BaseApplication.takeContext()

                return DefaultConnectivityHandler.obtain(ctx)
            }
        }
    )
{

    companion object :

        SingleInstance<ConnectionAvailableService>(),
        Obtainer<AvailableStatefulService> {

        override fun instantiate(): ConnectionAvailableService {

            return InternetConnectionAvailabilityService()
        }

        override fun getObtainer(): Obtain<AvailableStatefulService> {

            return object : Obtain<AvailableStatefulService> {

                override fun obtain() = instantiate()
            }
        }
    }


    override val tag: String = "${identifier()} ::"

    override fun identifier() = "Internet connection availability"
}