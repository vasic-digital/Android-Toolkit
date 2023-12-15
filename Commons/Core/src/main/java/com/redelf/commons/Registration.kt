package com.redelf.commons

interface Registration<T> {

    fun register(subscriber: T)

    fun unregister(subscriber: T)

    fun isRegistered(subscriber: T): Boolean
}