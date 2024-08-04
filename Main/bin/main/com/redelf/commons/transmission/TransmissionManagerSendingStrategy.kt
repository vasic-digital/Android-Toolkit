package com.redelf.commons.transmission

interface TransmissionManagerSendingStrategy<T> {

    fun isReady(): Boolean

    fun isNotReady() = !isReady()

    fun executeSending(data: T): Boolean
}