/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


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
