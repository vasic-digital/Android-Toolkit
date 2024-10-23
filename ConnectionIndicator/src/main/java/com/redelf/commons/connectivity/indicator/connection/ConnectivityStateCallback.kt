package com.redelf.commons.connectivity.indicator.connection

import com.redelf.commons.net.connectivity.ConnectionState
import com.redelf.commons.net.connectivity.ConnectivityStateChanges
import com.redelf.commons.stateful.State

abstract class ConnectivityStateCallback : ConnectivityStateChanges {

    final override fun setState(state: State<Int>) = Unit

    final override fun getState() = ConnectionState.Disconnected
}