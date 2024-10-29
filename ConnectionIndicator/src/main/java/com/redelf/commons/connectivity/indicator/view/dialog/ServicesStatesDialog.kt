package com.redelf.commons.connectivity.indicator.view.dialog

import android.app.Activity
import android.content.DialogInterface
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.redelf.commons.connectivity.indicator.R
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulServices
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulServicesBuilder
import com.redelf.commons.extensions.exec
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

    builder: AvailableStatefulServicesBuilder,
    private val serviceCallback: ServicesStatesDialogCallback

) : BaseDialog(ctx, dialogStyle) {

    override val layout = dialogLayout
    override val tag = "Connectivity :: Indicator :: Dialog ::"

    private var adapter: ServicesStatesDialogAdapter? = null
    private var statefulServices: AvailableStatefulServices? = null

    init {

        exec(

            onRejected = { err -> recordException(err) }

        ) {

            try {

                statefulServices = AvailableStatefulServices(builder)

            } catch (e: Exception) {

                recordException(e)
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)

        Console.log("$tag On dismiss :: START")

        adapter?.dismiss()

        Console.log("$tag On dismiss :: Adapter dismissed")

        statefulServices?.getServiceInstances()?.forEach { service ->

            if (service is TerminationAsync) {

                service.terminate("On dismiss")

                Console.log("$tag Service terminated :: ${service::class.simpleName}")

            } else if (service is TerminationSynchronized) {

                service.terminate("On dismiss")

                Console.log("$tag Service terminated :: ${service::class.simpleName}")

            } else {

                val msg = "Service cannot be terminated ${service.javaClass.simpleName}"
                val e = IllegalStateException(msg)
                recordException(e)

                Console.error("$tag On dismiss :: $msg")
            }
        }

        adapter = null

        Console.log("$tag On dismiss :: END")
    }

    override fun onContentView(contentView: View) {

        val items = statefulServices?.getServiceInstances() ?: emptyList()
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