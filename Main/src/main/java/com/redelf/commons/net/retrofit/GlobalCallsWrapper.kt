package com.redelf.commons.net.retrofit

import com.redelf.commons.interruption.Abort
import com.redelf.commons.logging.Console
import okhttp3.Call
import java.util.concurrent.ConcurrentHashMap

object GlobalCallsWrapper : Abort {

    val CALLS = ConcurrentHashMap<String, Call>()

    override fun abort() {

        val tag = "GlobalCallsWrapper :: Abort ::"

        Console.log("$tag START")

        CALLS.forEach { (k, v) ->

            Console.log("$tag Cancel :: $k")

            try {

                v.cancel()

            } catch (e: Exception) {

                Console.error("$tag Cancel failed: $k", e)
            }
        }

        CALLS.clear()

        Console.log("$tag END")
    }
}