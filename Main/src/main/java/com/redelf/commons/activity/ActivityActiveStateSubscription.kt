package com.redelf.commons.activity

interface ActivityActiveStateSubscription {

    fun register(subscriber: ActivityActiveStateListener)

    fun unregister(subscriber: ActivityActiveStateListener)

    fun isRegistered(subscriber: ActivityActiveStateListener): Boolean
}