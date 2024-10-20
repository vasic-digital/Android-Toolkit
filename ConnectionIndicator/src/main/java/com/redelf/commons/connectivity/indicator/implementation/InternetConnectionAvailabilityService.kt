package com.redelf.commons.connectivity.indicator.implementation

import android.content.Context
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.connectivity.indicator.connection.ConnectionAvailableService
import com.redelf.commons.context.ContextAvailability
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.logging.Console
import com.redelf.commons.net.connectivity.ConnectionState
import com.redelf.commons.net.connectivity.ConnectivityStateChanges
import com.redelf.commons.net.connectivity.DefaultConnectivityHandler
import com.redelf.commons.stateful.State

class InternetConnectionAvailabilityService :

    ConnectionAvailableService(),
    ContextAvailability<Context>,
    TerminationSynchronized

{

    private val connectionHandler = DefaultConnectivityHandler(takeContext())

    private val connectionCallback = object : ConnectivityStateChanges {

        override fun onStateChanged() {

            this@InternetConnectionAvailabilityService.onStateChanged()
        }

        override fun onState(state: State<Int>) {

            Console.log("On state: $state")

            this@InternetConnectionAvailabilityService.onState(state)
        }

        override fun getState(): State<Int> {

            if (connectionHandler.isNetworkAvailable(takeContext())) {

                return ConnectionState.Connected
            }

            return ConnectionState.Disconnected
        }

        override fun setState(state: State<Int>) {

            this@InternetConnectionAvailabilityService.setState(state)
        }
    }

    init {

        connectionHandler.register(connectionCallback)
    }

    override fun takeContext() = BaseApplication.takeContext()

    override fun terminate(): Boolean {

        connectionHandler.unregister(connectionCallback)

        return true
    }

    override fun onStateChanged() {

        // TODO: Implement onStateChanged()
    }

    override fun onState(state: State<Int>) {

        setState(state)

        // TODO: Implement onState()
    }
}