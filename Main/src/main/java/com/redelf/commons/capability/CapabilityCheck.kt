package com.redelf.commons.capability

fun interface CapabilityCheck {

    fun checkCapability(callback: CapabilityCheckCallback)
}