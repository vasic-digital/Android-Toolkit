package com.redelf.commons.net.connectivity

import com.redelf.commons.stateful.State

enum class STATE : State {

    Disconnected,
    Connected,
    Connecting
}