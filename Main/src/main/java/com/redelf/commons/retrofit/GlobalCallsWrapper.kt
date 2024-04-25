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

        CALLS.values.forEach { it.cancel() }
        CALLS.clear()

        Timber.v("$tag END")
    }
}