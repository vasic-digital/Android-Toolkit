package com.redelf.commons.retrofit

import com.redelf.commons.interruption.Abort
import okhttp3.Call
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

object GlobalCallsWrapper : Abort {

    val CALLS = ConcurrentHashMap<String, Call>()

    override fun abort() {

        val tag = "GlobalCallsWrapper :: Abort ::"

        Timber.v("$tag START")

        CALLS.forEach { (k, v) ->

            Timber.v("$tag Cancel: $k")

            try {

                if (v.isCanceled() || v.isExecuted()) {

                    Timber.v("$tag Skipped: $k")

                } else {

                    v.cancel()

                    Timber.v("$tag Canceled: $k")
                }

            } catch (e: Exception) {

                Timber.e("$tag Cancel failed: $k", e)
            }
        }

        CALLS.clear()

        Timber.v("$tag END")
    }
}