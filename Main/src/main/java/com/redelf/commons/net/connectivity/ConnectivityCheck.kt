package com.redelf.commons.net.connectivity

import android.content.Context

interface ConnectivityCheck {

    fun isNetworkAvailable(ctx: Context, from: String): Boolean

    fun requireNetworkAvailable(ctx: Context, from: String): Boolean
}