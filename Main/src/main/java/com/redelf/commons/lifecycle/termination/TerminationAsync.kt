package com.redelf.commons.lifecycle.termination

interface TerminationAsync : Termination {

    fun terminate(vararg args: Any = emptyArray())
}