package com.redelf.commons.api

import android.content.Context
import com.redelf.commons.R
import com.redelf.commons.connectivity.Connectivity
import com.redelf.commons.connectivity.ConnectivityCheck
import com.redelf.commons.extensions.retrofitApiParameters
import com.redelf.commons.retrofit.RetrofitApiParameters

abstract class ApiService<T> (

    protected val ctx: Context,
    protected val connectivity: ConnectivityCheck = Connectivity(),

) {

    protected open val retrofitApiParameters: RetrofitApiParameters = retrofitApiParameters(

        ctx = ctx,
        endpoint = R.string.retrofit_endpoint,

        readTimeoutInSeconds = 60,
        connectTimeoutInSeconds = 60,
        writeTimeoutInSeconds = 2 * 60
    )

    protected abstract val apiService: T
}