package com.redelf.commons.connectivity.indicator.connection

import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.connectivity.indicator.AvailableService
import com.redelf.commons.net.connectivity.ConnectionState
import com.redelf.commons.net.connectivity.ConnectivityStateChanges
import com.redelf.commons.stateful.State
import java.util.concurrent.atomic.AtomicInteger

abstract class ConnectionAvailableService(identifier: String) : AvailableService, ConnectivityStateChanges {

    protected val tag = "$identifier ::"

    private val callbacks = Callbacks<ConnectivityStateChanges>(identifier)
    private val state: AtomicInteger = AtomicInteger(ConnectionState.Disconnected.getState())

    override fun getState() = ConnectionState.getState(state.get())

    override fun setState(state: State<Int>) {

        this.state.set(state.getState())

        notifyState(state)
        notifyStateChanged()
    }

    private fun notifyState(state: State<Int>) {

        callbacks.doOnAll(

            object : CallbackOperation<ConnectivityStateChanges> {

                override fun perform(callback: ConnectivityStateChanges) {

                    callback.onState(state)
                }
            },

            operationName = "state"
        )
    }

    private fun notifyStateChanged() {

        callbacks.doOnAll(

            object : CallbackOperation<ConnectivityStateChanges> {

                override fun perform(callback: ConnectivityStateChanges) {

                    callback.onStateChanged()
                }
            },

            operationName = "stateChanged"
        )
    }
}