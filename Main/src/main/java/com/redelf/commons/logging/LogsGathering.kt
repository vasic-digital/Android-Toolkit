package com.redelf.commons.logging

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.exec
import com.redelf.commons.recordException
import com.redelf.commons.toast
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.lang.StringBuilder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object LogsGathering {

    var TOAST = true
    var EMAIL = true
    var FS_LOG = true
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

        if (TOAST) {

            BaseApplication.CONTEXT.toast(value)
        }

        LOGS[key]?.add(value)
    }

    fun send(activity: Activity? = null) {

        if (!ENABLED) {

            Timber.w("Logs gathering is disabled")
            return
        }

        LOGS.forEach { (key, _) ->

            send(key, activity)
        }
    }

    fun send(key: String, activity: Activity? = null) {

        if (!ENABLED) {

            Timber.w("Logs gathering is disabled")
            return
        }

        val builder = StringBuilder(

            "\n$key :: START :: Version code: ${BaseApplication.getVersionCode()}"
        )

        LOGS[key]?.forEach { row ->

            builder.append("\n").append(row)
        }

        builder.append("\n$key :: END")

        val logs = builder.toString()

        Timber.v("GATHERED :: \n$logs")

        if (EMAIL) {

            activity?.let {

                sendEmail(it, key, logs)
            }
        }

        if (FS_LOG) {

            writeLog(key, logs)
        }

        val gatheredLogs = GatheredLogs(logs)
        recordException(gatheredLogs)

        LOGS[key] = CopyOnWriteArrayList()
    }

    private fun sendEmail(activity: Activity, key: String, logs: String) {

        Timber.v("Send email")

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {

            data = Uri.parse("mailto:")

            putExtra(Intent.EXTRA_SUBJECT, "$key logs ${System.currentTimeMillis()}")
            putExtra(Intent.EXTRA_TEXT, logs)
        }

        try {

            activity.startActivity(emailIntent)

        } catch (ex: ActivityNotFoundException) {

            activity.toast("There are no email clients installed")
        }
    }

    private fun writeLog(key: String, logs: String) {

        exec {

            val fileName = "$key.${System.currentTimeMillis()}.txt"

            val dir = Environment.DIRECTORY_DOWNLOADS
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(dir)
            val file = File(downloadsFolder, fileName)

            try {

                FileWriter(file).use { writer ->

                    writer.append(logs)
                }

                Timber.v("File written into: ${file.absolutePath}")

            } catch (e: IOException) {

                Timber.e(e)
            }
        }
    }
}