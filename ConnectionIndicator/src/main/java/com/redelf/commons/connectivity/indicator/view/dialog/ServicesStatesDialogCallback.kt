package com.redelf.commons.connectivity.indicator.view.dialog

import com.redelf.commons.connectivity.indicator.AvailableService

interface ServicesStatesDialogCallback {

    fun onService(service: AvailableService)
}