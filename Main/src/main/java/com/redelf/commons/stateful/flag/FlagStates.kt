package com.redelf.commons.stateful.flag

import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.Throws

object FlagStates {

    private val STATES = ConcurrentHashMap<String, FlagState>()

    fun getState(name: String): FlagState {

        return STATES.getOrPut(name) {

            FlagState(name)
        }
    }

    fun removeState(name: String): Boolean {

        if (STATES.contains(name)) {

            return STATES.remove(name) != null
        }

        return true
    }

    @Throws(IllegalStateException::class)
    fun resetState(name: String): FlagState {

        if (removeState(name)) {

            return getState(name)
        }

        throw IllegalStateException("Failed to reset '$name' state")
    }
}