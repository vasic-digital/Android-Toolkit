package com.redelf.commons.security.cleanup

interface ApplicationCleanupCallback {

    fun onCleanup(success: Boolean)
}