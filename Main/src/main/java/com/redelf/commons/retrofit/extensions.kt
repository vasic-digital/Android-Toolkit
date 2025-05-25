package com.redelf.commons.retrofit

import com.redelf.commons.logging.Console
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