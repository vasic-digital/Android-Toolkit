package com.redelf.commons.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.WorkSource
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.execution.BackgroundTaskWorker
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

val DEBUG_EXEC_EXTENSION = AtomicBoolean()
val KEEP_ALIVE_DURATION = AtomicLong(5 * 60_000) // 5 minutes

private val KEEPING_ALIVE = AtomicBoolean()

@Throws(RejectedExecutionException::class)
fun ThreadPoolExecutor.exec(label: String, what: Runnable) {

    var tag = ""
    var start = 0L

    if (DEBUG_EXEC_EXTENSION.get()) {

        tag = "Exec :: $label ::"
        start = System.currentTimeMillis()

        Console.log("$tag START")
    }

    this.execute(what)

    if (DEBUG_EXEC_EXTENSION.get()) {

        val time = System.currentTimeMillis() - start

        if (time >= 500) {

            Console.error("$tag END :: Duration = $time millis")

        } else if (time >= 200) {

            Console.warning("$tag END :: Duration = $time millis")

        } else {

            Console.log("$tag END :: Duration = $time millis")
        }
    }
}

@Suppress("DEPRECATION")
@SuppressLint("Wakelock")
fun executeWithWakeLock(

    enabled: Boolean = BaseApplication.takeContext().canWakeLock(),
    delay: Long = 0,
    duration: Long = 30_000,
    dismiss: Boolean = true,
    onError: (e: Throwable) -> Unit = { e -> recordException(e) },
    block: () -> Unit

) {

    var wakeLock: PowerManager.WakeLock? = null

    try {

        if (!enabled) {

            block()

            return
        }

        val tag = "WakeLockExecute::$duration"
        val ctx = BaseApplication.takeContext()
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager?

        if (pm?.isInteractive == true) {

            block()

            return
        }

        wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag)?.apply {

            setWorkSource(WorkSource())

            acquire(duration)
        }

    } catch (e: Throwable) {

        onError(e)
    }

    try {

        yieldWhile(timeoutInMilliseconds = delay) { true }

        block()

    } finally {

        try {

            if (dismiss) {

                wakeLock?.release()
            }

        } catch (e: Throwable) {

            onError(e)
        }
    }
}

fun executeWithWorkManager(

    force: Boolean = false,
    enabled: Boolean = BaseApplication.takeContext().canWorkManager(),
    onError: (e: Throwable) -> Unit = { e -> recordException(e) },
    block: () -> Unit

) {

    try {

        if (!enabled) {

            block()

            return
        }

        val ctx = BaseApplication.takeContext()
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager?

        if (pm?.isInteractive == true && !force) {

            block()

            return
        }

        BackgroundTaskWorker.setTask(block)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<BackgroundTaskWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInitialDelay(0, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(ctx).enqueue(workRequest)

    } catch (e: Throwable) {

        onError(e)
    }
}

fun onWorker(runnable: Runnable) {

    executeWithWorkManager {

        runnable.run()
    }
}

fun <T> syncOnWorkerJava(obtainable: Obtain<T?>): T? {

    return runBlocking {

        syncOnWorker(obtainable)
    }
}

suspend fun <T> syncOnWorker(

    obtainable: Obtain<T?>,
    context: CoroutineContext = Dispatchers.IO

): T? = withContext(context) {

    suspendCoroutine { continuation ->

        executeWithWakeLock(

            delay = 500,
            dismiss = false

        ) {

            BaseApplication.takeContext().wakeUpScreen {

                executeWithWorkManager(

                    onError = { e ->

                        continuation.resumeWithException(e)
                    }

                ) {

                    try {

                        val result = obtainable.obtain()

                        continuation.resume(result)

                    } catch (e: Throwable) {

                        continuation.resumeWithException(e)
                    }
                }
            }
        }
    }
}

fun keepAlive(with: Any? = null) {

    val tag = "Keep alive :: With='${with?.javaClass?.simpleName}' ::"

    if (KEEPING_ALIVE.get()) {

        Console.warning("$tag Already keeping alive")

        return
    }

    Console.log("$tag PRE-START")

    onWorker {

        Console.log("$tag START")

        try {

            var step = 0L
            val now = System.currentTimeMillis()

            while (System.currentTimeMillis() - now <= KEEP_ALIVE_DURATION.get()) {

                if (!KEEPING_ALIVE.get()) {

                    KEEPING_ALIVE.set(true)
                }

                yieldWhile(timeoutInMilliseconds = 5000) { true }

                with?.let {

                    val hash = it.hashCode()

                    Console.log("$tag Step=$step, Hash=$hash")
                }

                if (with == null) {

                    Console.log("$tag Step=$step")
                }

                step++
            }

            KEEPING_ALIVE.set(false)

            Console.log("$tag END")

        } catch (e: Throwable) {

            Console.error("$tag END :: Error='${e.message ?: e::class.simpleName}'")
        }
    }
}

fun isInputStreamOpen(inputStream: InputStream?): Boolean {

    try {

        if (inputStream != null) {

            if (inputStream.markSupported()) {

                inputStream.mark(1)

                val byteRead = inputStream.read()

                if (byteRead != -1) {

                    inputStream.reset()
                }

            } else {

                inputStream.available()
            }

            return true
        }
    } catch (e: IOException) {

        return false

    } catch (e: NullPointerException) {

        return false
    }

    return false
}

fun isDeviceInDozeMode(context: Context): Boolean {

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isDeviceIdleMode
}
