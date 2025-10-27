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


package com.redelf.commons.connectivity.indicator.connection

import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulService
import com.redelf.commons.lifecycle.termination.TerminationAsync
import com.redelf.commons.logging.Console
import com.redelf.commons.net.connectivity.ConnectionState
import com.redelf.commons.stateful.State
import com.redelf.commons.stateful.Stateful
import java.util.concurrent.atomic.AtomicInteger

abstract class ConnectionAvailableService :

    AvailableStatefulService,
    TerminationAsync

{

    private var stateChangesListeners: Callbacks<Stateful>? = null
    private val state: AtomicInteger = AtomicInteger(ConnectionState.Disconnected.getState())

    protected abstract fun identifier(): String

    override fun setDebug(debug: Boolean) {

        stateChangesListeners?.setDebug(debug)
    }

    override fun isDebug(): Boolean {

        return stateChangesListeners?.isDebug() ?: false
    }

    override fun getState() = ConnectionState.getState(lastState())

    override fun setState(state: State<Int>) {

        Console.log("${tag()} Set state :: State = $state, Received")

        val stateValue = state.getState()

        if (lastState() != stateValue) {

            this.state.set(stateValue)

            notifyStateSubscribers(state)

            Console.log("${tag()} Set state :: State = $state, Set")

        } else {

            Console.log("${tag()} Set state :: State = $state, Skipped")
        }
    }

    override fun terminate(vararg args: Any) {

        Console.log("${tag()} Terminate :: START :: Args = ${args.joinToString()}")

        callbacks().doOnAll(

            object : CallbackOperation<Stateful> {

                override fun perform(callback: Stateful) {

                    callbacks().unregister(callback)
                }

            }, operationName = "Termination"
        )

        Console.log("${tag()} Terminate :: END")
    }

    protected open fun tag() = "${identifier()} ::"

    protected fun lastState() = state.get()

    protected fun lastConnectionState() = ConnectionState.getState(lastState())

    protected open fun notifyStateSubscribers(state: State<Int>) {

        notifyState(state)
        notifyStateChanged()
    }

    private fun notifyState(state: State<Int>) {

        val callbacks = callbacks()

        Console.log(

            "${tag()} Notify state :: State = $state, " +
                "Callbacks count = ${callbacks.size()}"
        )

        callbacks.doOnAll(

            object : CallbackOperation<Stateful> {

                override fun perform(callback: Stateful) {

                    callback.onState(state, this@ConnectionAvailableService::class.java)
                }
            },

            operationName = "state"
        )
    }

    private fun notifyStateChanged() {

        val callbacks = callbacks()

        Console.log(

            "${tag()} Notify state changed :: State = $state, " +
                    "Callbacks count = ${callbacks.size()}"
        )

        callbacks.doOnAll(

            object : CallbackOperation<Stateful> {

                override fun perform(callback: Stateful) {

                    callback.onStateChanged(this@ConnectionAvailableService::class.java)
                }
            },

            operationName = "stateChanged"
        )
    }

    override fun register(subscriber: Stateful) {

        callbacks().register(subscriber)
    }

    override fun unregister(subscriber: Stateful) {

        callbacks().unregister(subscriber)
    }

    override fun isRegistered(subscriber: Stateful): Boolean {

        return callbacks().isRegistered(subscriber)
    }

    
    private fun callbacks(): Callbacks<Stateful> {

        if (stateChangesListeners == null) {

            stateChangesListeners = Callbacks(identifier())
        }

        return stateChangesListeners!!
    }
}
