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

import com.redelf.commons.authentification.exception.CredentialsInvalidException
import com.redelf.commons.obtain.OnObtain
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class DefaultApiServiceDefaultResponseHandler<T> : ApiServiceResponseHandler<T>() {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    override fun onResponse(

        response: Response<T?>?,
        callback: OnObtain<T?>,
        useExpectedCodes: Boolean,
        additionalExpectedCodes: List<Int>

    ) {

        val body = response?.body()
        val code = response?.code() ?: 0
        val combinedExpectedCodes = expectedCodes + additionalExpectedCodes

        when {

            code == 401 -> {

                callback.onFailure(CredentialsInvalidException())
            }

            code in 500..599 -> {

                callback.onFailure(IOException("Internal Server Error with code $code"))
            }

            response?.isSuccessful == true && body != null -> {

                callback.onCompleted(body)
            }

            useExpectedCodes && combinedExpectedCodes.contains(code) -> {

                callback.onCompleted(null)
            }

            additionalExpectedCodes.contains(code) -> {

                callback.onCompleted(null)
            }

            else -> {

                val error = if (DEBUG.get()) {

                    val url = response?.raw()?.request?.url

                    val errorBody = try {

                        response?.errorBody()?.string()

                    } catch (e: IOException) {

                        "Unable to read error body: ${e.message}"
                    }

                    IOException("Request failed with code $code\nURL: $url\nError: $errorBody")

                } else {

                    IOException("Request failed with code $code")
                }

                callback.onFailure(error)
            }
        }
    }
}