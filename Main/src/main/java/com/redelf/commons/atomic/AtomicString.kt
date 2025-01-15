package com.redelf.commons.atomic

import java.util.concurrent.atomic.AtomicReference

class AtomicString(initialValue: String) {

    private val atomicReference = AtomicReference(initialValue)

    fun get(): String = atomicReference.get()

    fun set(newValue: String) {

        atomicReference.set(newValue)
    }

    fun compareAndSet(expect: String, update: String): Boolean {

        return atomicReference.compareAndSet(expect, update)
    }
}