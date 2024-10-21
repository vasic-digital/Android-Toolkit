package com.redelf.commons.connectivity.indicator.connection

import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.connectivity.indicator.AvailableService
import com.redelf.commons.lifecycle.TerminationAsync
import com.redelf.commons.logging.Console
import com.redelf.commons.net.connectivity.ConnectionState
import com.redelf.commons.net.connectivity.ConnectivityStateChanges
import com.redelf.commons.registration.Registration
import com.redelf.commons.stateful.State
import java.util.concurrent.atomic.AtomicInteger

abstract class ConnectionAvailableService :

    AvailableService,
    TerminationAsync,
    ConnectivityStateChanges,
    Registration<ConnectivityStateChanges>

{

    private val callbacks = Callbacks<ConnectivityStateChanges>(identifier())
    private val state: AtomicInteger = AtomicInteger(ConnectionState.Disconnected.getState())

    protected abstract fun identifier(): String

    override fun getState() = ConnectionState.getState(state.get())

    override fun setState(state: State<Int>) {

        Console.log("${tag()} Set state :: START :: State = $state, Received")

        val stateValue = state.getState()

        if (this.state.get() != stateValue) {

            this.state.set(stateValue)

            notifyStateSubscribers(state)

            Console.log("${tag()} Set state :: START :: State = $state, Set")

        } else {

            Console.log("${tag()} Set state :: START :: State = $state, Skipped")
        }
    }

    override fun terminate() {

        callbacks.clear()
    }

    protected fun tag() = "${identifier()} ::"

    protected open fun notifyStateSubscribers(state: State<Int>) {

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

    override fun register(subscriber: ConnectivityStateChanges) {

        callbacks.register(subscriber)
    }

    override fun unregister(subscriber: ConnectivityStateChanges) {

        callbacks.unregister(subscriber)
    }

    override fun isRegistered(subscriber: ConnectivityStateChanges): Boolean {

        return callbacks.isRegistered(subscriber)
    }
}
