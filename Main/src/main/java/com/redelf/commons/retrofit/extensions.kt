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


package com.redelf.commons.retrofit

import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import okhttp3.Headers
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response

fun <T> Response<T>.close() {

    val tag = "Response :: $this ::"

    try {

        val raw = this.raw()

        Console.log("$tag Closing")

        raw.close()

        Console.log("$tag Closed")

        if (raw == null) {

            Console.warning("$tag No raw response to close")
        }

    } catch (_: Exception) {

        // Ignore
    }
}

class ResponseWrapper<T>(

    var response: Response<T>? = null,
    var body: T? = null,
    var errorBody: ResponseBody? = null

) {

    fun code(): Int = response?.code() ?: -1

    fun headers(): Headers? = response?.headers()

    fun isSuccessful() = response?.isSuccessful == true
}

fun <T> Call<T>.safeExecute(): ResponseWrapper<T> {

    val tag = "Retrofit"
    var response: Response<T>? = null
    val wrapper = ResponseWrapper<T>()

    try {

        Console.log("$tag executing")

        response = this.execute()

        Console.log("$tag executed")

        wrapper.response = response
        wrapper.body = response?.body()
        wrapper.errorBody = response?.errorBody()

    } catch (e: Throwable) {

        recordException(e)

        response?.close()
    }

    return wrapper
}