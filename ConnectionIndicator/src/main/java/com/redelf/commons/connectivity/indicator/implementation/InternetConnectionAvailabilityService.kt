package com.redelf.commons.connectivity.indicator.implementation

import android.content.Context
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.connectivity.indicator.connection.ConnectionAvailableService
import com.redelf.commons.context.ContextAvailability
import com.redelf.commons.stateful.State

class InternetConnectionAvailabilityService :

    ConnectionAvailableService(),
    ContextAvailability<Context>

{

    override fun takeContext() = BaseApplication.takeContext()

    override fun onStateChanged() {

        TODO("Not yet implemented")
    }

    override fun onState(state: State<Int>) {

        TODO("Not yet implemented")
    }
}