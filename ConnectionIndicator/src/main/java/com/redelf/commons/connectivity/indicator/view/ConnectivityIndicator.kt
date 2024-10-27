package com.redelf.commons.connectivity.indicator.view

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.redelf.commons.connectivity.indicator.AvailableService
import com.redelf.commons.connectivity.indicator.R
import com.redelf.commons.connectivity.indicator.connection.ConnectivityStateCallback
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulServices
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulServicesBuilder
import com.redelf.commons.connectivity.indicator.view.dialog.ServicesStatesDialog
import com.redelf.commons.connectivity.indicator.view.dialog.ServicesStatesDialogCallback
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.recordException
import com.redelf.commons.lifecycle.InitializationAsyncParametrized
import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.lifecycle.TerminationAsync
import com.redelf.commons.logging.Console
import com.redelf.commons.net.connectivity.ConnectionState
import com.redelf.commons.net.connectivity.Reconnectable
import com.redelf.commons.stateful.State
import java.util.concurrent.atomic.AtomicBoolean

class ConnectivityIndicator :

    RelativeLayout,
    InitializationAsyncParametrized<AvailableStatefulServices, AvailableStatefulServicesBuilder>,
    TerminationAsync

{

    var dialogStyle = 0
    var showDetails = false
    var dialogLayout = R.layout.dialog_services_states
    var dialogAdapterItemLayout: Int = R.layout.layout_services_states_dialog_adapter

    var colorStateWarning = R.color.warning
    var colorStateConnected = R.color.connected
    var colorStateUnavailable = R.color.unavailable
    var colorStateDisconnected = R.color.disconnected

    var iconConnectedState = R.drawable.ic_link_on
    var iconDisconnectedState = R.drawable.ic_link_off

    private val initializing = AtomicBoolean()
    private val tag = "Connectivity Indicator ::"
    private var dialog: ServicesStatesDialog? = null
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

    private val serviceCallback = object : ServicesStatesDialogCallback {

        private val tag = "${this@ConnectivityIndicator.tag} Dialog callback ::"

        override fun onService(service: AvailableService) {

            if (service is Reconnectable) {

                Console.log("$tag Service = ${service::class.simpleName}")

                service.reconnect()

                dialog?.dismiss()
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

    override fun onFinishInflate() {
        super.onFinishInflate()

        Console.log("$tag On finish inflate")

        LayoutInflater.from(context).inflate(layout, this, true)

        applyStates()
    }

    override fun onDetachedFromWindow() {

        dialog?.dismiss()

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

    /*
    *
    *   FIXME:
    *    - Make sure that there is a repeatable task to refresh the state of the indicator (and the dialog if visible)
    *    - Make sure that FCM is refreshed with internet connectivity events / connectivity state changes
    *
    */
    private fun applyStates() {

        Console.log("$tag Apply states")

        doApplyStates()

        if (showDetails) {

            val button = findViewById<ImageButton?>(R.id.button)

            button.setOnClickListener {

                doApplyStates()
                presentServiceState()
            }
        }
    }

    private fun doApplyStates() {

        val state = statefulServices?.getState()
        val button = findViewById<ImageButton?>(R.id.button)

        val tint = when (state) {

            ConnectionState.Connected -> {

                ContextCompat.getColor(context, colorStateConnected)
            }

            ConnectionState.Disconnected -> {

                ContextCompat.getColor(context, colorStateDisconnected)
            }

            ConnectionState.Warning -> {

                ContextCompat.getColor(context, colorStateWarning)
            }

            ConnectionState.Unavailable -> {

                ContextCompat.getColor(context, colorStateUnavailable)
            }

            else -> {

                ContextCompat.getColor(context, colorStateDisconnected)
            }
        }

        Console.log("$tag Tint color: $tint")

        val icon = when (state) {

            ConnectionState.Connected -> {

                iconConnectedState
            }

            ConnectionState.Disconnected -> {

                iconDisconnectedState
            }

            ConnectionState.Warning -> {

                iconConnectedState
            }

            else -> {

                iconDisconnectedState
            }
        }

        button.setImageResource(icon)
        button?.setColorFilter(tint)
    }

    private fun presentServiceState() {

        Console.log("$tag Present service state")

        if (context is Activity) {

            statefulServices?.let {

                val ctx = context as Activity

                dialog = ServicesStatesDialog(

                    ctx,
                    dialogStyle,
                    dialogLayout,
                    dialogAdapterItemLayout,
                    services = it,
                    serviceCallback = serviceCallback
                )

                dialog?.show()
            }

        } else {

            Console.warning("$tag Context is not an Activity")
        }
    }
}