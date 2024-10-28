package com.redelf.commons.connectivity.indicator.view.dialog

import android.app.Activity
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.redelf.commons.connectivity.indicator.R
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulServices
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

    override fun onContentView(contentView: View) {

        val items = services.getServiceInstances()
        val recycler = contentView.findViewById<RecyclerView?>(R.id.services)

        recycler?.let {

            val adapter = ServicesStatesDialogAdapter(

                items,
                dialogAdapterItemLayout,
                serviceCallback
            )

            it.addOnAttachStateChangeListener(

                object : View.OnAttachStateChangeListener {

                    override fun onViewAttachedToWindow(v: View) {

                        // Ignore
                    }

                    override fun onViewDetachedFromWindow(v: View) {

                        items.forEach { service ->

                            if (service is TerminationAsync) {

                                Console.log("$tag Service = ${service::class.simpleName}")

                                service.terminate()
                            }

                            if (service is TerminationSynchronized) {

                                Console.log("$tag Service = ${service::class.simpleName}")

                                service.terminate()
                            }
                        }
                    }
                }
            )

            it.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(takeContext())
            it.adapter = adapter
        }
    }
}