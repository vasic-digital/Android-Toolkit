package com.redelf.commons.connectivity.indicator.view.dialog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.redelf.commons.connectivity.indicator.R
import com.redelf.commons.connectivity.indicator.AvailableService
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulServicesBuilder
import com.redelf.commons.connectivity.indicator.view.ConnectivityIndicator
import com.redelf.commons.net.connectivity.Reconnectable

class ServicesStatesDialogAdapter(

    private val services: List<AvailableService>,
    private val layout: Int = R.layout.layout_services_states_dialog_adapter,
    private val serviceCallback: ServicesStatesDialogCallback

) : RecyclerView.Adapter<ServicesStatesDialogAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val title = view.findViewById<TextView?>(R.id.title)
        val refresh = view.findViewById<ImageButton?>(R.id.refresh)
        val bottomSeparator = view.findViewById<View?>(R.id.bottom_separator)
        val indicator = view.findViewById<ConnectivityIndicator?>(R.id.indicator)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(viewGroup.context).inflate(layout, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val service = services[position]

        viewHolder.refresh?.setOnClickListener {

            if (service is Reconnectable) {

                serviceCallback.onService(service)
            }
        }

        viewHolder.refresh?.isEnabled = service is Reconnectable

        viewHolder.title?.text = service.getWho()

        val builder = AvailableStatefulServicesBuilder()
            .addService(service::class.java)
            .setDebug(true)

        viewHolder.indicator?.setServices(builder)

        if (position < services.size - 1) {

            viewHolder.bottomSeparator?.visibility = View.VISIBLE

        } else {

            viewHolder.bottomSeparator?.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount() = services.size
}