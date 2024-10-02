package com.redelf.commons.extensions

import android.content.Context
import com.redelf.commons.logging.Console
import com.redelf.commons.net.retrofit.RetrofitApiParameters

fun retrofitApiParameters(

    name: String,
    ctx: Context,
    endpoint: Int,

    readTimeoutInSeconds: Long = 30,
    connectTimeoutInSeconds: Long = 30,
    writeTimeoutInSeconds: Long = -1,

    scalar: Boolean? = false,
    jackson: Boolean? = false,

    verbose: Boolean? = false,
    bodyLog: Boolean? = false

): RetrofitApiParameters {

    val params = RetrofitApiParameters(

        ctx = ctx,
        name = name,
        useCronet = false,
        endpoint = endpoint,
        readTimeoutInSeconds = readTimeoutInSeconds,
        writeTimeoutInSeconds = writeTimeoutInSeconds,
        connectTimeoutInSeconds = connectTimeoutInSeconds,

        bodyLog = bodyLog ?: false,
        verbose = verbose ?: false,

        scalar = scalar ?: false,
        jackson = jackson ?: false
    )

    Console.log("Retrofit :: Parameters :: Service = '${params.name}' :: $params")

    return params
}