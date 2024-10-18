package com.redelf.commons.connectivity.indicator.implementation

import android.content.Context
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.connectivity.indicator.connection.ConnectionAvailableService
import com.redelf.commons.context.ContextAvailability

class InternetConnectionAvailabilityService :

    ConnectionAvailableService,
    ContextAvailability<Context>

{

    override fun takeContext() = BaseApplication.takeContext()


}