package com.redelf.commons.connectivity.indicator.view.dialog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.redelf.commons.connectivity.indicator.AvailableService
import com.redelf.commons.connectivity.indicator.R
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulServicesBuilder
import com.redelf.commons.connectivity.indicator.view.ConnectivityIndicator
import com.redelf.commons.creation.instantiation.SingleInstantiated
import com.redelf.commons.dismissal.Dismissable
import com.redelf.commons.extensions.recordException
import com.redelf.commons.lifecycle.TerminationAsync
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.logging.Console
import com.redelf.commons.net.connectivity.Reconnect
import java.util.concurrent.CopyOnWriteArraySet

class ServicesStatesDialogAdapter(

    private val services: List<AvailableService>,
    private val textColor: Int? = null,
    private val layout: Int = R.layout.layout_services_states_dialog_adapter,
    private val serviceCallback: ServicesStatesDialogCallback

) : RecyclerView.Adapter<ServicesStatesDialogAdapter.ViewHolder>(), Dismissable {

    private val servicesObjects = CopyOnWriteArraySet<AvailableService>()
    private val tag = "Connectivity :: Indicator :: Dialog :: Adapter ::"

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val title = view.findViewById<TextView?>(R.id.title)
        val refresh = view.findViewById<ImageButton?>(R.id.refresh)
        val bottomSeparator = view.findViewById<View?>(R.id.bottom_separator)
        val indicator = view.findViewById<ConnectivityIndicator?>(R.id.indicator)
    }

    override fun dismiss() {

        Console.log("$tag Dismissing :: START")

        fun logServiceTermination(service: AvailableService) = Console.log(

            "$tag Service :: Termination :: OK :: ${service::class.simpleName} " +
                    "- ${service.hashCode()}"
        )

        fun logServiceSkipped(service: AvailableService) = Console.warning(

            "$tag Service :: Termination :: SKIPPED :: ${service::class.simpleName} " +
                    "- ${service.hashCode()}"
        )

        servicesObjects.forEach { service ->

            if (service is TerminationAsync) {

                if (service is SingleInstantiated) {

                    logServiceSkipped(service)

                } else {

                    service.terminate("On dismiss")

                    logServiceTermination(service)
                }

            } else if (service is TerminationSynchronized) {

                if (service is SingleInstantiated) {

                    logServiceSkipped(service)

                } else {

                    service.terminate("On dismiss")

                    logServiceTermination(service)
                }

            } else {

                val msg = "Service cannot be terminated ${service.javaClass.simpleName}"
                val e = IllegalStateException(msg)
                recordException(e)

                Console.error("$tag ERROR :: $msg")
            }
        }

        Console.log("$tag Dismissing :: END")
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(viewGroup.context).inflate(layout, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val service = services[position]

        viewHolder.refresh?.setOnClickListener {

            if (service is Reconnect) {

                serviceCallback.onService(service)
            }
        }

        viewHolder.refresh?.isEnabled = service is Reconnect

        if (service is Reconnect) {

            viewHolder.refresh?.visibility = View.VISIBLE

        } else {

            viewHolder.refresh?.visibility = View.INVISIBLE
        }

        viewHolder.title?.text = service.getWho()

        textColor?.let {

            viewHolder.title?.setTextColor(it)
        }

        val origin = this@ServicesStatesDialogAdapter::class.java.simpleName

        val builder = AvailableStatefulServicesBuilder(origin)
            .addService(service::class.java)
            .setDebug(true)

        viewHolder.indicator?.origin = origin
        viewHolder.indicator?.setServices(builder)

        val newObjects = viewHolder.indicator?.getServices()?.getServiceInstances() ?: emptyList()

        servicesObjects.addAll(newObjects)

        Console.log(

            "$tag Terminate :: Added service objects = " +
                    "${newObjects.size} / ${servicesObjects.size}"
        )

        if (position < services.size - 1) {

            viewHolder.bottomSeparator?.visibility = View.VISIBLE

        } else {

            viewHolder.bottomSeparator?.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount() = services.size
}