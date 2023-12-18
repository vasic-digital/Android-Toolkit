package com.redelf.commons.security.check

interface SafetyCheckCallback {

    fun onRootingCheck(isRooted: Boolean)
}