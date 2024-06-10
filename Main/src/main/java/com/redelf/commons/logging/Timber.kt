package com.redelf.commons.logging

import com.redelf.commons.application.BaseApplication
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

object Timber {

    private val recordLogs = AtomicBoolean(false)

    @JvmStatic
    fun initialize(logsRecording: Boolean = false) {

        setLogsRecording(logsRecording)

        if (logsRecording) {

            val appName = BaseApplication.getName()
            val appVersion = BaseApplication.getVersion()
            val appVersionCode = BaseApplication.getVersionCode()
            val recordingFileName = "$appName-$appVersion-$appVersionCode"

            Timber.plant(RecordingTree(recordingFileName))

        } else {

            Timber.plant(Timber.DebugTree())
        }
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

    private fun setLogsRecording(enabled: Boolean) {

        Timber.i("Set logs recording: $enabled")

        recordLogs.set(enabled)
    }
}