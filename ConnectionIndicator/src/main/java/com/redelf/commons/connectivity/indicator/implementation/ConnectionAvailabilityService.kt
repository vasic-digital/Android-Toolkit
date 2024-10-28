package com.redelf.commons.connectivity.indicator.implementation

import android.content.Context
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.connectivity.indicator.connection.ConnectionAvailableService
import com.redelf.commons.context.ContextAvailability
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.isOnMainThread
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.net.connectivity.ConnectionState
import com.redelf.commons.net.connectivity.ConnectivityHandler
import com.redelf.commons.net.connectivity.ConnectivityStateChanges
import com.redelf.commons.net.connectivity.StatefulBasicConnectionHandler
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.refreshing.AutoRefreshing
import com.redelf.commons.stateful.State
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

abstract class ConnectionAvailabilityService(

    private val handlerObtain: Obtain<StatefulBasicConnectionHandler>

) : ContextAvailability<Context>, ConnectionAvailableService(), AutoRefreshing {

    protected abstract val tag: String
    protected val autRefresh = AtomicBoolean(true)
    protected open val refreshingFrequency = 1000L

    private var timer: Timer? = null
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

    private val timerTask = object : TimerTask() {

        override fun run() {

            if (Thread.currentThread().isInterrupted) {

                return
            }

            Console.log("${refreshingTag()} RUN")

            checkState()
        }
    }

    init {

        withConnectionHandler {

            Console.log("${tag()} Instantiated :: ${hashCode()}")

            startRefreshing()
        }
    }

    override fun takeContext() = BaseApplication.takeContext()

    override fun terminate() {

        stopRefreshing()

        super.terminate()

        val tag = "${tag()} Termination ::"

        Console.log("$tag START")

        withConnectionHandler {

            Console.log("$tag Handler available :: ${it.hashCode()}")

            terminateHandler(it)
        }
    }

    override fun onStateChanged(whoseState: Class<*>?) {

        withConnectionHandler {

            if (it.isNetworkAvailable(takeContext())) {

                onState(ConnectionState.Connected, whoseState)

            } else {

                onState(ConnectionState.Disconnected, whoseState)
            }
        }
    }

    override fun onState(state: State<Int>, whoseState: Class<*>?) {

        setState(state)
    }

    override fun startRefreshing() {

        stopRefreshing()

        Console.log("${refreshingTag()} PRE-START")

        if (autRefresh.get()) {

            Console.log("${refreshingTag()} START")

            timer = Timer()

            timer?.schedule(timerTask, refreshingFrequency, refreshingFrequency)

        } else {

            Console.log("${refreshingTag()} SKIP")
        }
    }

    override fun stopRefreshing() {

        Console.log("${refreshingTag()} STOP")

        timer?.cancel()
        timer?.purge()
        timer = null
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

        if (handler is StatefulBasicConnectionHandler) {

            Console.log("$tag Type ok")

            handler.unregister(connectionCallback)

            Console.log("$tag END")
        }
    }

    private fun checkState() {

        val tag = "$tag Check state ::"

        if (isDebug()) {

            Console.log("$tag START")
        }

        val state = getState()
        val last = lastConnectionState()
        val whose = this@ConnectionAvailabilityService::class.java

        if (state != last) {

            Console.log("$tag CHANGED :: $last -> $state")

            onStateChanged(whose)
        }

        onState(state, whose)

        if (isDebug()) {

            Console.log("$tag END")
        }
    }

    private fun refreshingTag() = "${tag()} Refreshing :: ${hashCode()} ::"
}