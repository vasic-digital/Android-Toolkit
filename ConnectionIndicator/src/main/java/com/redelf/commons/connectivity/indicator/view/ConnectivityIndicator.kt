package com.redelf.commons.connectivity.indicator.view

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.redelf.commons.connectivity.indicator.connection.ConnectivityStateCallback
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulServices
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulServicesBuilder
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.recordException
import com.redelf.commons.lifecycle.InitializationAsyncParametrized
import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.logging.Console
import com.redelf.commons.net.connectivity.ConnectionState
import com.redelf.commons.stateful.State
import java.util.concurrent.atomic.AtomicBoolean

class ConnectivityIndicator :

    RelativeLayout,
    InitializationAsyncParametrized<AvailableStatefulServices, AvailableStatefulServicesBuilder> {

    private val initializing = AtomicBoolean()
    private val tag = "Connectivity Indicator ::"
    private var statefulServices: AvailableStatefulServices? = null

    private val connectionStateCallback = object : ConnectivityStateCallback() {

        override fun onStateChanged(whoseState: Class<*>?) {

            Console.log(

                "$tag State has changed :: Who = ${whoseState?.simpleName}"
            )
        }

        override fun onState(state: State<Int>, whoseState: Class<*>?) {

            if (state.getState() == ConnectionState.Connected.getState()) {

                Console.log(

                    "$tag Connected to the internet :: " +
                            "Who = ${whoseState?.simpleName}"
                )

            } else {

                Console.log(

                    "$tag Disconnected from the internet :: " +
                            "Who = ${whoseState?.simpleName}"
                )
            }
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

    fun getServices(): AvailableStatefulServices? {

        return statefulServices
    }

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
}