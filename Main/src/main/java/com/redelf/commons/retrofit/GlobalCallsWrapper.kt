package com.redelf.commons.retrofit

import com.redelf.commons.interruption.Abort
import com.redelf.commons.logging.Timber
import okhttp3.Call
import java.util.concurrent.ConcurrentHashMap

object GlobalCallsWrapper : Abort {

    val CALLS = ConcurrentHashMap<String, Call>()

    override fun abort() {

        val tag = "GlobalCallsWrapper :: Abort ::"

        Timber.v("$tag START")

        CALLS.forEach { (k, v) ->

            Timber.v("$tag Cancel :: $k")

            try {

                v.cancel()

            } catch (e: Exception) {

                Timber.e("$tag Cancel failed: $k", e)
            }
        }

        CALLS.clear()

        Timber.v("$tag END")
    }
}