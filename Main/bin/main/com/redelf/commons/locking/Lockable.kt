package com.redelf.commons.locking

interface Lockable {

    fun lock()

    fun unlock()

    fun isLocked(): Boolean

    fun isUnlocked(): Boolean
}