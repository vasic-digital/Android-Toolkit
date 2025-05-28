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