package com.redelf.commons.connectivity.indicator.implementation

import android.content.Context
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.connectivity.indicator.connection.ConnectionAvailableService
import com.redelf.commons.connectivity.indicator.connection.ConnectivityStateCallback
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulService
import com.redelf.commons.context.ContextAvailability
import com.redelf.commons.dependency.Chainable
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.isOnMainThread
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.net.connectivity.ConnectionState
import com.redelf.commons.net.connectivity.ConnectivityHandler
import com.redelf.commons.net.connectivity.ConnectivityStateChanges
import com.redelf.commons.net.connectivity.StatefulBasicConnectionHandler
import com.redelf.commons.obtain.suspendable.Obtain
import com.redelf.commons.stateful.State
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

abstract class ConnectionAvailabilityService(

    private val origin: String,
    private val handlerObtain: Obtain<StatefulBasicConnectionHandler>

) :

    ContextAvailability<Context>,
    ConnectionAvailableService()

{

    private val chained: CopyOnWriteArraySet<AvailableStatefulService> = CopyOnWriteArraySet()

    private val connectionCallback = object : ConnectivityStateChanges {

        private val tag =
            this@ConnectionAvailabilityService.tag() + " Connection callback ::"

        override fun onStateChanged(whoseState: Class<*>?) {

            Console.log("$tag On state changed :: '${whoseState?.simpleName}'")

            this@ConnectionAvailabilityService.onStateChanged(whoseState)
        }

        override fun onState(state: State<Int>, whoseState: Class<*>?) {

            Console.log(

                "$tag On state, calling the change callback :: '${whoseState?.simpleName}'"
            )

            this@ConnectionAvailabilityService.onStateChanged(whoseState)
        }

        @Throws(IllegalArgumentException::class, IllegalStateException::class)
        override fun getState(): State<Int> {

            if (isOnMainThread()) {

                throw IllegalArgumentException("Cannot get state from main thread")
            }

            val latch = CountDownLatch(1)
            var state = ConnectionState.Disconnected

            withConnectionHandler {

                if (it.isNetworkAvailable(takeContext())) {

                    state = ConnectionState.Connected

                    latch.countDown()
                }
            }

            try {

                val result = latch.await(30, TimeUnit.SECONDS)

                if (!result) {

                    throw IllegalStateException("Get state timeout")
                }

            } catch (e: Exception) {

                recordException(e)

                throw IllegalStateException("Cannot get state")
            }

            return state
        }

        override fun setState(state: State<Int>) {

            this@ConnectionAvailabilityService.setState(state)
        }
    }

    protected var cHandler: StatefulBasicConnectionHandler? = null

    private val chainedStateListener = object : ConnectivityStateCallback() {

        override fun onStateChanged(whoseState: Class<*>?) = Unit

        override fun onState(

            state: State<Int>,
            whoseState: Class<*>?

        ) {

            val who = this@ConnectionAvailabilityService
            val cumulativeState = who.getState()

            if (state != cumulativeState) {

                who.onStateChanged(who::class.java)
            }

            who.onState(

                cumulativeState,
                who::class.java
            )
        }
    }

    init {

        withConnectionHandler {

            Console.log("${tag()} Instantiated :: ${hashCode()}")
        }
    }

    override fun tag(): String {

        return "${super.tag()} Origin = $origin ::"
    }

    override fun takeContext() = BaseApplication.takeContext()

    override fun terminate(vararg args: Any) {

        unchainAll()

        super.terminate(*args)

        val tag = "${tag()} Termination :: Args = ${args.joinToString()}"

        Console.log("$tag START")

        withConnectionHandler {

            Console.log("$tag Handler available :: ${it.hashCode()}")

            terminateHandler(it)
        }
    }

    override fun chain(what: AvailableStatefulService):
            Chainable<AvailableStatefulService> {

        chained.add(what)

        what.register(chainedStateListener)

        return this
    }

    override fun unchain(what: AvailableStatefulService):
            Chainable<AvailableStatefulService> {

        what.unregister(chainedStateListener)

        chained.remove(what)

        return this
    }

    override fun onStateChanged(whoseState: Class<*>?) {

        withConnectionHandler {

            if (it.isNetworkAvailable(takeContext())) {

                val chainedOk = getChainedResult()

                if (chainedOk) {

                    onState(ConnectionState.Connected, whoseState)

                } else {

                    onState(ConnectionState.Disconnected, whoseState)
                }

            } else {

                onState(ConnectionState.Disconnected, whoseState)
            }
        }
    }

    override fun onState(state: State<Int>, whoseState: Class<*>?) {

        if (state == ConnectionState.Disconnected) {

            setState(state)
            return
        }

        val chainedOk = getChainedResult()

        if (chainedOk) {

            setState(state)

        } else {

            setState(ConnectionState.Disconnected)
        }
    }

    final override fun getState(): ConnectionState {

        cHandler?.let {

            if (it.isNetworkAvailable(takeContext())) {

                val chainedOk = getChainedResult()

                if (chainedOk) {

                    return ConnectionState.Connected

                } else {

                    setState(ConnectionState.Disconnected)
                }
            }
        }

        return ConnectionState.Disconnected
    }

    protected fun withConnectionHandler(doWhat: (handler: ConnectivityHandler) -> Unit) {

        exec(

            onRejected = { err -> recordException(err) }

        ) {

            if (cHandler == null) {

                cHandler = handlerObtain.obtain()

                val state = if (cHandler?.isNetworkAvailable(takeContext()) == true) {

                    ConnectionState.Connected

                } else {

                    ConnectionState.Disconnected
                }

                setState(state)

                cHandler?.register(connectionCallback)
            }

            cHandler?.let {

                doWhat(it)
            }
        }
    }

    private fun terminateHandler(handler: ConnectivityHandler) {

        Console.log("${tag()} Terminate handler :: START")

        if (handler is StatefulBasicConnectionHandler) {

            Console.log("${tag()} Terminate handler :: TYPE OK")

            handler.unregister(connectionCallback)

            Console.log("${tag()} Terminate handler :: END")

        } else {

            Console.error(

                "${tag()} Terminate handler :: ERROR: " +
                    "Unsupported type ${handler::class.simpleName}"
            )
        }
    }

    private fun unchainAll() {

        chained.forEach {

            unchain(it)
        }
    }

    private fun getChainedResult(): Boolean {

        chained.forEach {

            if (it.getState() != ConnectionState.Connected) {

                return false
            }
        }

        return true
    }
}