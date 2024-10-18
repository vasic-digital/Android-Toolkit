package com.redelf.commons.connectivity.indicator.connection

import com.redelf.commons.connectivity.indicator.AvailableService
import com.redelf.commons.net.connectivity.ConnectionState
import com.redelf.commons.net.connectivity.ConnectivityStateChanges
import com.redelf.commons.stateful.State
import java.util.concurrent.atomic.AtomicInteger

abstract class ConnectionAvailableService : AvailableService, ConnectivityStateChanges {

    private val state: AtomicInteger = AtomicInteger(ConnectionState.Disconnected.getState())

    override fun getState() = ConnectionState.getState(state.get())

    override fun setState(state: State<Int>) {

        this.state.set(state.getState())
    }
}