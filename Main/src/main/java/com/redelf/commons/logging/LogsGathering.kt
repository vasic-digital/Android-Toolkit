package com.redelf.commons.logging

import com.redelf.commons.recordException
import java.lang.StringBuilder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object LogsGathering {

    private val LOGS = ConcurrentHashMap<String, CopyOnWriteArrayList<String>>()

    fun addLog(key: String, value: String) {

        var list = LOGS[key]

        if (list == null) {

            list = CopyOnWriteArrayList()
            LOGS[key] = list
        }

        LOGS[key]?.add(value)
    }

    fun send() {

        LOGS.forEach{ (key, value) ->

            val builder = StringBuilder("$key --- START")

            value.forEach { row ->

                builder.append("\n").append(row)
            }

            builder.append("\n$key --- END")

            val gatheredLogs = GatheredLogs(builder.toString())
            recordException(gatheredLogs)
        }
    }

}