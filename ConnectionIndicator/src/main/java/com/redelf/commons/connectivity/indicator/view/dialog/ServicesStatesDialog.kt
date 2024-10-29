package com.redelf.commons.connectivity.indicator.view.dialog

import android.app.Activity
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.redelf.commons.connectivity.indicator.R
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulServices
import com.redelf.commons.extensions.recordException
import com.redelf.commons.lifecycle.TerminationAsync
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.logging.Console
import com.redelf.commons.ui.dialog.BaseDialog

class ServicesStatesDialog(

    ctx: Activity,
    dialogStyle: Int = 0,
    dialogLayout: Int = R.layout.dialog_services_states,
    private val dialogAdapterItemLayout: Int = R.layout.layout_services_states_dialog_adapter,

    private val services: AvailableStatefulServices,
    private val serviceCallback: ServicesStatesDialogCallback

) : BaseDialog(ctx, dialogStyle) {

    override val tag = "Services states dialog ::"

    override val layout = dialogLayout

    private var adapter: ServicesStatesDialogAdapter? = null

    override fun dismiss() {

        adapter?.dismiss()

        services.getServiceInstances().forEach { service ->

            if (service is TerminationAsync) {

                Console.log("$tag Service = ${service::class.simpleName}")

                service.terminate()

            } else if (service is TerminationSynchronized) {

                Console.log("$tag Service = ${service::class.simpleName}")

                service.terminate()

            } else {

                val msg = "Service cannot be terminated ${service.javaClass.simpleName}"
                val e = IllegalStateException(msg)
                recordException(e)
            }
        }

        super.dismiss()
    }

    override fun onContentView(contentView: View) {

        val items = services.getServiceInstances()
        val recycler = contentView.findViewById<RecyclerView?>(R.id.services)

        recycler?.let {

            adapter = ServicesStatesDialogAdapter(

                items,
                dialogAdapterItemLayout,
                serviceCallback
            )

            it.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(takeContext())
            it.adapter = adapter
        }
    }
}