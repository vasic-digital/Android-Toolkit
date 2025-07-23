package com.redelf.commons.stateful.flag

import java.util.concurrent.ConcurrentHashMap

object FlagStates {

    private val STATES = ConcurrentHashMap<String, FlagState>()

    fun getState(name: String): FlagState {

        return STATES.getOrPut(name) {

            FlagState(name)
        }
    }
}