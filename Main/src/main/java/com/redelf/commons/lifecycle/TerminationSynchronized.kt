package com.redelf.commons.lifecycle

interface TerminationSynchronized : Termination {

    fun terminate(vararg args: Any = emptyArray()): Boolean
}