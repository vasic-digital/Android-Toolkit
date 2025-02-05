package com.redelf.commons.extensions

import android.content.Context
import com.redelf.commons.net.retrofit.RetrofitApiParameters

fun retrofitApiParameters(

    name: String,
    ctx: Context,
    endpoint: Int,

    readTimeoutInSeconds: Long = 30,
    connectTimeoutInSeconds: Long = 30,
    writeTimeoutInSeconds: Long = 30,

    scalar: Boolean? = false,
    jackson: Boolean? = false,

    verbose: Boolean? = false,
    bodyLog: Boolean? = false

): RetrofitApiParameters {

    return RetrofitApiParameters(

        ctx = ctx,
        name = name,
        useCronet = false,
        endpoint = endpoint,
        readTimeoutInSeconds = readTimeoutInSeconds,
        writeTimeoutInSeconds = writeTimeoutInSeconds,
        connectTimeoutInSeconds = connectTimeoutInSeconds,

        bodyLog = bodyLog == true,
        verbose = verbose == true,

        scalar = scalar == true,
        jackson = jackson == true
    )
}