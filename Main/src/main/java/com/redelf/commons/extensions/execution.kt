package com.redelf.commons.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkRequest
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
private var WAKE_LOCK: PowerManager.WakeLock? = null

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
    duration: Long = 10 * 60 * 1000L,
    onError: (e: Throwable) -> Unit = { e -> recordException(e) },
    block: () -> Unit

) {

    var wakeLock: PowerManager.WakeLock? = null

    try {

        if (!enabled) {

            block()

            return
        }

    } catch (e: Throwable) {

        onError(e)
    }

    acquireWakeLock(duration)

    try {

        yieldWhile(timeoutInMilliseconds = delay) { true }

        block()

    } catch (e: Throwable) {

        onError(e)
    }
}

fun executeWithWorkManager(

    delay: Long = 0,
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

        if (ctx.isVeryOldDevice()) {

            block()

            return
        }

        BackgroundTaskWorker.setTask(block)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)  // Important for Doze
            .setRequiresCharging(false)       // Important for Doze
            .setRequiresDeviceIdle(false)     // CRITICAL: Don't require device idle!
            .build()

        val workRequest = OneTimeWorkRequestBuilder<BackgroundTaskWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(constraints)
            .setBackoffCriteria(

                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.SECONDS
            )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        val operation = WorkManager.getInstance(ctx).enqueue(workRequest)

        onWorkManagerOperation(operation, ctx, block)

    } catch (e: Throwable) {

        onError(e)
    }
}

fun onWorkManagerOperation(

    operation: Operation,
    ctx: Context = BaseApplication.takeContext(),
    block: () -> Unit

) {

    operation.result.addListener(

        {

            try {

                val result = operation.result.get()

                if (result is Operation.State.SUCCESS) {

                    Console.log("Work manager operation succeeded")

                } else {

                    val e = Throwable("Work manager operation failed")
                    recordException(e)

                    executeWithWakeLock(

                        onError = { e ->

                            recordException(e)

                            block()
                        }

                    ) {

                        block()
                    }
                }

            } catch (e: Exception) {

                recordException(e)

                executeWithWakeLock(

                    onError = { e ->

                        recordException(e)

                        block()
                    }

                ) {

                    block()
                }
            }
        },

        ContextCompat.getMainExecutor(ctx)
    )
}

fun onWorker(runnable: Runnable) {

    executeWithWorkManager(

        onError = {

                e ->
            recordException(e)

            runnable.run()
        }

    ) {

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

            delay = 500

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

@SuppressLint("Wakelock")
fun acquireWakeLock(duration: Long = 10 * 60 * 1000L) { // 10 minutes

    try {

        releaseWakeLock()

        val powerManager =
            BaseApplication.takeContext().getSystemService(POWER_SERVICE) as PowerManager?

        WAKE_LOCK = powerManager?.newWakeLock(

            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "PlayerService::WakeLock"
        )

        WAKE_LOCK?.acquire(duration)

    } catch (e: Throwable) {

        recordException(e)
    }
}

fun releaseWakeLock(wLock: PowerManager.WakeLock? = WAKE_LOCK) {

    try {

        if (wLock?.isHeld == true) {

            wLock.release()
        }

        if (wLock == WAKE_LOCK) {

            WAKE_LOCK = null
        }

    } catch (e: Throwable) {

        Console.error(e)
    }
}