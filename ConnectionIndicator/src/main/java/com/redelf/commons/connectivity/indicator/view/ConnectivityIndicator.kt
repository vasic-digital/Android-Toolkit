package com.redelf.commons.connectivity.indicator.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.redelf.commons.connectivity.indicator.R
import com.redelf.commons.connectivity.indicator.connection.ConnectivityStateCallback
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulServices
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulServicesBuilder
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.recordException
import com.redelf.commons.lifecycle.InitializationAsyncParametrized
import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.lifecycle.TerminationAsync
import com.redelf.commons.logging.Console
import com.redelf.commons.net.connectivity.ConnectionState
import com.redelf.commons.stateful.State
import java.util.concurrent.atomic.AtomicBoolean

class ConnectivityIndicator :

    RelativeLayout,
    InitializationAsyncParametrized<AvailableStatefulServices, AvailableStatefulServicesBuilder>,
    TerminationAsync

{

    private val initializing = AtomicBoolean()
    private val tag = "Connectivity Indicator ::"
    private val layout = R.layout.layout_connectivity_indicator
    private var statefulServices: AvailableStatefulServices? = null

    private val connectionStateCallback = object : ConnectivityStateCallback() {

        override fun onStateChanged(whoseState: Class<*>?) {

            Console.log(

                "$tag State has changed :: Who = ${whoseState?.simpleName}"
            )

            applyStates()
        }

        override fun onState(state: State<Int>, whoseState: Class<*>?) {

            Console.log("$tag State = $state, whoseState = ${whoseState?.simpleName}")
        }
    }

    constructor(ctx: Context) : super(ctx)

    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)

    constructor(

        ctx: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int

    ) : super(ctx, attrs, defStyleAttr)

    constructor(

        ctx: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int

    ) : super(ctx, attrs, defStyleAttr, defStyleRes)

    override fun onFinishInflate() {
        super.onFinishInflate()

        Console.log("$tag On finish inflate")

        LayoutInflater.from(context).inflate(layout, this, true)

        applyStates()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        Console.log("$tag On detached from window")

        terminate()
    }

    override fun terminate() {

        exec(

            onRejected = { error ->

                recordException(error)
            }

        ) {

            statefulServices?.terminate()
            statefulServices = null
        }
    }

    fun getServices(): AvailableStatefulServices? {

        return statefulServices
    }

    // TODO: Tint colors to be configurable
    // TODO: Set services method version to inject already build services stack

    fun setServices(

        services: AvailableStatefulServicesBuilder,
        callback: LifecycleCallback<AvailableStatefulServices>? = null

    ) {

        val callbackWrapper = object : LifecycleCallback<AvailableStatefulServices> {

            override fun onInitialization(

                success: Boolean,
                vararg args: AvailableStatefulServices

            ) {

                callback?.onInitialization(success = success, *args)
            }

            override fun onShutdown(success: Boolean, vararg args: AvailableStatefulServices) {

                callback?.onShutdown(success = success, *args)
            }
        }

        initialize(services, callbackWrapper)
    }

    override fun initialize(

        param: AvailableStatefulServicesBuilder,
        callback: LifecycleCallback<AvailableStatefulServices>

    ) {

        exec(

            onRejected = { error ->

                recordException(error)

                callback.onInitialization(success = false)
            }

        ) {

            initializing.set(true)

            try {

                param.addCallback(connectionStateCallback)

                statefulServices = AvailableStatefulServices(param)

                callback.onInitialization(true, statefulServices!!)

            } catch (e: Exception) {

                param.removeCallback(connectionStateCallback)

                recordException(e)

                callback.onInitialization(success = false)
            }

            initializing.set(false)
        }
    }

    override fun isInitialized(): Boolean {

        return statefulServices != null
    }

    override fun isInitializing(): Boolean {

        return initializing.get()
    }

    override fun initializationCompleted(e: Exception?) {

        e?.let {

            Console.log("$tag ERROR: ${e.message}")
            Console.log(e)
        }

        if (e == null) {

            Console.log("$tag Initialization completed")
        }
    }

    private fun applyStates() {

        Console.log("$tag Apply states")

        val state = statefulServices?.getState()

        val tint = when (state) {

            ConnectionState.Connected -> {

                ContextCompat.getColor(context, R.color.connected)
            }

            ConnectionState.Disconnected -> {

                ContextCompat.getColor(context, R.color.disconnected)
            }

            ConnectionState.Warning -> {

                ContextCompat.getColor(context, R.color.warning)
            }

            ConnectionState.Unavailable -> {

                ContextCompat.getColor(context, R.color.unavailable)
            }

            else -> {

                ContextCompat.getColor(context, R.color.disconnected)
            }
        }

        Console.log("$tag Tint color: $tint")
    }
}