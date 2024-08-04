package com.redelf.commons.lifecycle

import com.redelf.commons.lifecycle.exception.InitializedException
import com.redelf.commons.lifecycle.exception.NotInitializedException
import com.redelf.commons.lifecycle.exception.ShuttingDownException
import com.redelf.commons.lifecycle.exception.TerminatedException
import java.util.concurrent.atomic.AtomicBoolean


class LifecycleCheck {

    private var initialized = AtomicBoolean()
    private var initializing = AtomicBoolean()
    private var shuttingDown = AtomicBoolean()

    fun setInitialized(state: Boolean) {

        initialized.set(state)
        initializing.set(false)
    }

    fun isInitialized() = initialized.get()

    fun setInitializing(state: Boolean) = initializing.set(state)

    fun isInitializing() = initializing.get()

    fun setShuttingDown(state: Boolean) = shuttingDown.set(state)

    fun isShuttingDown() = shuttingDown.get()

    @Throws(InitializedException::class)
    fun failOnInitialized() {

        if (isInitialized()) {

            throw InitializedException()
        }
    }

    @Throws(TerminatedException::class)
    fun failOnTerminated() {

        if (!isInitialized()) {

            throw TerminatedException()
        }
    }

    @Throws(IllegalStateException::class)
    fun readyCheck() {

        initializationCheck()
        shutdownCheck()
    }


    @Throws(NotInitializedException::class)
    fun initializationCheck() {

        if (!isInitialized()) {

            throw NotInitializedException()
        }
    }

    @Throws(ShuttingDownException::class)
    fun shutdownCheck() {

        if (isShuttingDown()) {

            throw ShuttingDownException()
        }
    }
}
