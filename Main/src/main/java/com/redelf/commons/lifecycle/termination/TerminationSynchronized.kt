package com.redelf.commons.lifecycle.termination

interface TerminationSynchronized : Termination {

    fun terminate(vararg args: Any = emptyArray()): Boolean
}