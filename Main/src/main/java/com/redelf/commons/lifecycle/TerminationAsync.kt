package com.redelf.commons.lifecycle

interface TerminationAsync : Termination {

    fun terminate(vararg args: Any = emptyArray())
}