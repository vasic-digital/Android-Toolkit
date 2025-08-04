package com.redelf.commons.lifecycle

import com.redelf.commons.lifecycle.termination.Termination

interface TerminationSynchronizedParametrized : Termination {

    fun terminate(vararg args: Any = emptyArray()): Boolean
}