package com.redelf.commons.lifecycle

interface TerminationSynchronizedParametrized : Termination {

    fun terminate(vararg args: Any = emptyArray()): Boolean
}