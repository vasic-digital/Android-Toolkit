package com.redelf.commons.net.connectivity

import com.redelf.commons.stateful.State

enum class ConnectionState(private val stateValue: Int) : State<Int> {

    Connected(0),
    Connecting(1),
    Warning(2),
    Disconnected(-1),
    Unavailable(-2);

    companion object {

        fun getState(from: Int): ConnectionState {

            entries.forEach {

                if (it.stateValue == from) {

                    return it
                }
            }

            return Disconnected
        }
    }

    override fun getState(): Int = stateValue
}