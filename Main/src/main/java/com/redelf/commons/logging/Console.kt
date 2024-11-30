package com.redelf.commons.logging

import com.redelf.commons.application.BaseApplication
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

object Console : LogParametrized {

    fun filesystemGranted(): Boolean {

        return RecordingTree.filesystemGranted()
    }

    private var tree: Timber.Tree? = null
    private val production = AtomicBoolean(false)
    private val recordLogs = AtomicBoolean(false)
    private val failOnError = AtomicBoolean(false)

    @JvmStatic
    fun initialize(

        logsRecording: Boolean = false,
        failOnError: Boolean = false,
        production: Boolean = false,

    ) {

        if (logsRecording && !filesystemGranted()) {

            return
        }

        setFailOnError(failOnError)
        setLogsRecording(logsRecording)

        this.production.set(production)

        if (logsRecording) {

            val appName = BaseApplication.getName()
            val appVersion = BaseApplication.getVersion()
            val appVersionCode = BaseApplication.getVersionCode()
            val recordingFileName = "$appName-$appVersion-$appVersionCode"

            tree = RecordingTree(recordingFileName, production = production)

            tree?.let {

                Timber.plant(it)

                (it as RecordingTree).hello()
            }

        } else {

            tree = Timber.DebugTree()

            tree?.let {

                Timber.plant(it)
            }
        }
    }

    override fun logParametrized(priority: Int, tag: String?, message: String, t: Throwable?) {

        if (tree is LogParametrized) {

            (tree as LogParametrized).logParametrized(priority, tag, message, t)

        } else {

            tree?.log(priority, tag, message, t)
        }
    }

    @JvmStatic
    fun log(message: String?, vararg args: Any?) {

        if (production.get()) {

            logParametrized(0, "", "$message :: Args = ${args.toList()}", null)

            return
        }

        Timber.v(message, *args)
    }

    @JvmStatic
    fun log(t: Throwable?, message: String?, vararg args: Any?) {

        if (production.get()) {

            logParametrized(0, "", "$message :: Args = ${args.toList()}", t)

            return
        }

        Timber.v(t, message, *args)
    }

    @JvmStatic
    fun log(t: Throwable?) {

        if (production.get()) {

            logParametrized(0, "", "", t)

            return
        }

        Timber.v(t)
    }

    @JvmStatic
    fun debug(message: String?, vararg args: Any?) {

        if (production.get()) {

            logParametrized(1, "", "$message :: Args = ${args.toList()}", null)

            return
        }

        Timber.d(message, *args)
    }

    @JvmStatic
    fun debug(t: Throwable?, message: String?, vararg args: Any?) {

        if (production.get()) {

            logParametrized(1, "", "$message :: Args = ${args.toList()}", t)

            return
        }

        Timber.d(t, message, *args)
    }

    @JvmStatic
    fun debug(t: Throwable?) {

        if (production.get()) {

            logParametrized(1, "", "", t)

            return
        }

        Timber.d(t)
    }

    @JvmStatic
    fun info(message: String?, vararg args: Any?) {

        if (production.get()) {

            logParametrized(2, "", "$message :: Args = ${args.toList()}", null)

            return
        }

        Timber.i(message, *args)
    }

    @JvmStatic
    fun info(t: Throwable?, message: String?, vararg args: Any?) {

        if (production.get()) {

            logParametrized(2, "", "$message :: Args = ${args.toList()}", t)

            return
        }

        Timber.i(t, message, *args)
    }

    @JvmStatic
    fun info(t: Throwable?) {

        if (production.get()) {

            logParametrized(2, "", "", t)

            return
        }

        Timber.i(t)
    }

    @JvmStatic
    fun warning(message: String?, vararg args: Any?) {

        if (production.get()) {

            logParametrized(3, "", "$message :: Args = ${args.toList()}", null)

            return
        }

        Timber.w(message, *args)
    }

    @JvmStatic
    fun warning(t: Throwable?, message: String?, vararg args: Any?) {

        if (production.get()) {

            logParametrized(3, "", "$message :: Args = ${args.toList()}", t)

            return
        }

        Timber.w(t, message, *args)
    }

    @JvmStatic
    fun warning(t: Throwable?) {

        if (production.get()) {

            logParametrized(3, "", "", t)

            return
        }

        Timber.w(t)
    }

    @JvmStatic
    fun error(message: String?, vararg args: Any?) {

        if (production.get()) {

            logParametrized(4, "", "$message :: Args = ${args.toList()}", null)

            return
        }

        Timber.e(message, *args)

        if (failOnError.get()) {

            throw RuntimeException(message)
        }
    }

    @JvmStatic
    fun error(t: Throwable?, message: String?, vararg args: Any?) {

        if (production.get()) {

            logParametrized(4, "", "$message :: Args = ${args.toList()}", t)

            return
        }

        Timber.e(t, message, *args)

        if (failOnError.get()) {

            throw RuntimeException(t)
        }
    }

    @JvmStatic
    fun error(t: Throwable?) {

        if (production.get()) {

            logParametrized(4, "", "", t)

            return
        }

        Timber.e(t)

        if (failOnError.get()) {

            throw RuntimeException(t)
        }
    }


    @JvmStatic
    fun log(priority: Int, message: String?, vararg args: Any?) {

        if (production.get()) {

            logParametrized(6, "", "$message :: Args = ${args.toList()}", null)

            return
        }

        Timber.log(priority, message, *args)
    }

    @JvmStatic
    fun log(priority: Int, t: Throwable?, message: String?, vararg args: Any?) {

        if (production.get()) {

            logParametrized(6, "", "$message :: Args = ${args.toList()}", t)

            return
        }

        Timber.log(priority, t, message, *args)
    }

    @JvmStatic
    fun log(priority: Int, t: Throwable?) {

        if (production.get()) {

            logParametrized(6, "", "", t)

            return
        }

        Timber.log(priority, t)
    }

    private fun setLogsRecording(enabled: Boolean) {

        Timber.i("Set logs recording: $enabled")

        recordLogs.set(enabled)
    }

    private fun setFailOnError(enabled: Boolean) {

        Timber.i("Set fail on error: $enabled")

        failOnError.set(enabled)
    }
}