package com.redelf.commons.connectivity.indicator.view.dialog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.redelf.commons.connectivity.indicator.R
import com.redelf.commons.connectivity.indicator.AvailableService

class ServicesStatesDialogAdapter(

    private val services: List<AvailableService>,
    private val layout: Int = R.layout.layout_services_states_dialog_adapter,
    private val serviceCallback: ServicesStatesDialogCallback

) : RecyclerView.Adapter<ServicesStatesDialogAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val title = view.findViewById<TextView?>(R.id.title)
        val refresh = view.findViewById<ImageButton?>(R.id.refresh)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(viewGroup.context).inflate(layout, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val service = services[position]

        viewHolder.refresh?.setOnClickListener {

            serviceCallback.onService(service)
        }

        viewHolder.title?.text = service.getWho()
    }

    override fun getItemCount() = services.size
}