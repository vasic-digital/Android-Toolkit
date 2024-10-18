package com.redelf.commons.connectivity.indicator.connection

import com.redelf.commons.stateful.State

enum class STATE : State {

    Offline,
    Connecting,
    Connected
}