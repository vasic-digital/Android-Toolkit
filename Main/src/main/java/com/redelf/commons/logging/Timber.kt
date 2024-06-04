package com.redelf.commons.logging

import android.content.Context
import android.os.Environment
import com.redelf.commons.R
import com.redelf.commons.extensions.exec
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

object Timber {

    private val recordLogs = AtomicBoolean(false)

    @JvmStatic
    fun initialize(ctx: Context) {

        val tag = "Timber :: Init ::"

        val resources = ctx.resources
        val recording = resources.getBoolean(R.bool.logs_gathering_enabled)

        setLogsRecording(recording)

        if (recording) {

            Timber.plant(RecordingTree())

        } else {

            Timber.plant(Timber.DebugTree())
        }

        Timber.v("$tag Recording: $recording")
    }

    @JvmStatic
    fun v(message: String?, vararg args: Any?) {

        Timber.v(message, *args)
    }

    @JvmStatic
    fun v(t: Throwable?, message: String?, vararg args: Any?) {

        Timber.v(t, message, *args)
    }

    @JvmStatic
    fun v(t: Throwable?) {

        Timber.v(t)
    }

    @JvmStatic
    fun d(message: String?, vararg args: Any?) {

        Timber.d(message, *args)
    }

    @JvmStatic
    fun d(t: Throwable?, message: String?, vararg args: Any?) {

        Timber.d(t, message, *args)
    }

    @JvmStatic
    fun d(t: Throwable?) {

        Timber.d(t)
    }

    @JvmStatic
    fun i(message: String?, vararg args: Any?) {

        Timber.i(message, *args)
    }

    @JvmStatic
    fun i(t: Throwable?, message: String?, vararg args: Any?) {

        Timber.i(t, message, *args)
    }

    @JvmStatic
    fun i(t: Throwable?) {

        Timber.i(t)
    }

    @JvmStatic
    fun w(message: String?, vararg args: Any?) {

        Timber.w(message, *args)
    }

    @JvmStatic
    fun w(t: Throwable?, message: String?, vararg args: Any?) {

        Timber.w(t, message, *args)
    }

    @JvmStatic
    fun w(t: Throwable?) {

        Timber.w(t)
    }

    @JvmStatic
    fun e(message: String?, vararg args: Any?) {

        Timber.e(message, *args)
    }

    @JvmStatic
    fun e(t: Throwable?, message: String?, vararg args: Any?) {

        Timber.e(t, message, *args)
    }

    @JvmStatic
    fun e(t: Throwable?) {

        Timber.e(t)
    }

    @JvmStatic
    fun wtf(message: String?, vararg args: Any?) {

        Timber.wtf(message, *args)
    }

    @JvmStatic
    fun wtf(t: Throwable?, message: String?, vararg args: Any?) {

        Timber.wtf(t, message, *args)
    }

    @JvmStatic
    fun wtf(t: Throwable?) {

        Timber.wtf(t)
    }

    @JvmStatic
    fun log(priority: Int, message: String?, vararg args: Any?) {

        Timber.log(priority, message, *args)
    }

    @JvmStatic
    fun log(priority: Int, t: Throwable?, message: String?, vararg args: Any?) {

        Timber.log(priority, t, message, *args)
    }

    @JvmStatic
    fun log(priority: Int, t: Throwable?) {

        Timber.log(priority, t)
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

    private fun setLogsRecording(enabled: Boolean) {

        Timber.i("Set logs recording: $enabled")

        recordLogs.set(enabled)
    }
}