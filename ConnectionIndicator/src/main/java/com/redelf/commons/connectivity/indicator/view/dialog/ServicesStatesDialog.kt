package com.redelf.commons.connectivity.indicator.view.dialog

import android.app.Activity
import android.view.View
import com.redelf.commons.connectivity.indicator.R
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulServices
import com.redelf.commons.ui.dialog.BaseDialog

class ServicesStatesDialog(

    ctx: Activity,
    dialogStyle: Int = 0,
    private val services: AvailableStatefulServices,
    private val serviceCallback: ServicesStatesDialogCallback

) : BaseDialog(ctx, dialogStyle) {

    override val tag = "Services states dialog ::"

    override val layout = R.layout.dialog_services_states

    override fun onContentView(contentView: View) {

        // TODO: Implement
    }
}