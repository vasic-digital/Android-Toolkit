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