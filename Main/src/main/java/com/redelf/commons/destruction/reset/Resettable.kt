package com.redelf.commons.destruction.reset

interface Resettable : Resetting {

    fun reset(): Boolean
}