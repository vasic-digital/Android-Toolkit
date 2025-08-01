package com.redelf.commons.lifecycle

interface TerminationSynchronized : Termination {

    fun terminate(): Boolean
}