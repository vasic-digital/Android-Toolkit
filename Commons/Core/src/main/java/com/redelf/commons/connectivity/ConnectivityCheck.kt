package com.redelf.commons.connectivity

import android.content.Context

interface ConnectivityCheck {

    fun isNetworkAvailable(ctx: Context): Boolean
}