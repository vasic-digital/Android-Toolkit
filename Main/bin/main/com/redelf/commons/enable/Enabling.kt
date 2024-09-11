package com.redelf.commons.enable

interface Enabling {

    fun isEnabled(): Boolean

    fun enable(callback: EnablingCallback? = null)

    fun disable(callback: EnablingCallback? = null)
}