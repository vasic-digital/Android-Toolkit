package com.redelf.commons.capability

fun interface CapabilityCheckCallback {

    fun onCapabilityChecked(capable: Boolean)
}