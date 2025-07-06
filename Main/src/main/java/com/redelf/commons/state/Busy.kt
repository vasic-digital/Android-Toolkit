package com.redelf.commons.state

interface Busy {

    fun isBusy(): Boolean

    fun setBusy(busy: Boolean)
}