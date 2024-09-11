package com.redelf.commons.enable

interface EnablingCallback {

    fun onChange(success: Boolean, isEnabled: Boolean)
}