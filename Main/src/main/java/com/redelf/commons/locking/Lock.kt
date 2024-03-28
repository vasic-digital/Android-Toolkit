package com.redelf.commons.locking

interface Lock {

    fun lock()

    fun unlock()

    fun isLocked(): Boolean
}