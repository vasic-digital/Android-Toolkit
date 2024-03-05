package com.redelf.commons.logging

import com.redelf.commons.recordException
import timber.log.Timber
import java.lang.StringBuilder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object LogsGathering {

    var ENABLED = false

    private val LOGS = ConcurrentHashMap<String, CopyOnWriteArrayList<String>>()

    fun addLog(key: String, value: String) {

        if (!ENABLED) {

            Timber.w("Logs gathering is disabled")
            return
        }

        var list = LOGS[key]

        if (list == null) {

            list = CopyOnWriteArrayList()
            LOGS[key] = list
        }

        LOGS[key]?.add(value)
    }

    fun send() {

        if (!ENABLED) {

            Timber.w("Logs gathering is disabled")
            return
        }

        LOGS.forEach { (key, _) ->

            send(key)
        }
    }

    fun send(key: String) {

        if (!ENABLED) {

            Timber.w("Logs gathering is disabled")
            return
        }

        val builder = StringBuilder("$key --- START")

        LOGS[key]?.forEach { row ->

            builder.append("\n").append(row)
        }

        builder.append("\n$key --- END")

        val logs = builder.toString()

        Timber.v("GATHERED :: \n$logs")

        val gatheredLogs = GatheredLogs(logs)
        recordException(gatheredLogs)

        LOGS[key] = CopyOnWriteArrayList()
    }
}