package com.redelf.commons.activity.stateful

interface ActivityActiveStateSubscription {

    fun register(subscriber: ActivityActiveStateListener)

    fun unregister(subscriber: ActivityActiveStateListener)

    fun isRegistered(subscriber: ActivityActiveStateListener): Boolean
}