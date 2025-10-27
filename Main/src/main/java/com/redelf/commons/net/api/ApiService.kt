/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


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