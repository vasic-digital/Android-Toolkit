package com.redelf.commons.net.api

import android.content.Context
import com.redelf.commons.extensions.retrofitApiParameters
import com.redelf.commons.net.connectivity.Connectivity
import com.redelf.commons.net.connectivity.ConnectivityCheck
import com.redelf.commons.net.retrofit.RetrofitApiParameters
import com.redelf.commons.service.Serving

abstract class ApiService<T> (

    endpoint: Int,
    serviceName: String,
    logApiCalls: Boolean = false,
    logApiCallsVerbose: Boolean = false,

    protected val ctx: Context,
    protected val connectivity: ConnectivityCheck = Connectivity(),

) : Serving {

    protected open val retrofitApiParameters: RetrofitApiParameters = retrofitApiParameters(

        ctx = ctx,
        name = serviceName,
        endpoint = endpoint,

        bodyLog = logApiCalls,
        verbose = logApiCallsVerbose,

        readTimeoutInSeconds = 60,
        connectTimeoutInSeconds = 60,
        writeTimeoutInSeconds = 2 * 60
    )

    protected abstract val apiService: T
}