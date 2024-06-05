package com.redelf.commons.extensions

import android.content.Context
import com.redelf.commons.R
import com.redelf.commons.retrofit.RetrofitApiParameters

fun retrofitApiParameters(

    ctx: Context,
    endpoint: Int,

    readTimeoutInSeconds: Long = 30,
    connectTimeoutInSeconds: Long = 30,
    writeTimeoutInSeconds: Long = -1,

    scalar: Boolean? = false,
    jackson: Boolean? = false,

    verbose: Boolean? = false,
    bodyLog: Boolean? = false

) = RetrofitApiParameters(

    ctx = ctx,
    readTimeoutInSeconds = readTimeoutInSeconds,
    writeTimeoutInSeconds = writeTimeoutInSeconds,
    connectTimeoutInSeconds = connectTimeoutInSeconds,
    endpoint = endpoint,

    bodyLog = bodyLog ?: false,
    verbose = verbose ?: false,

    scalar = scalar ?: false,
    jackson = jackson ?: false
)