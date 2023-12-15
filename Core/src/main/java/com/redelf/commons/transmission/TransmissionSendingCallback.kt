package com.redelf.commons.transmission

interface TransmissionSendingCallback<T> {

    fun onSendingStarted(data: T)

    fun onSent(data: T, success: Boolean)
}