package com.redelf.commons.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP
import android.os.PowerManager.FULL_WAKE_LOCK
import android.os.PowerManager.ON_AFTER_RELEASE
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
import kotlinx.coroutines.withContext
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

val DEBUG_EXEC_EXTENSION = AtomicBoolean()

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
fun Context.executeWithWakeLock(

    enabled: Boolean = BaseApplication.takeContext().canWakeLock(),
    duration: Long = 30000L,
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

        val tag = "WakeLockExecute.${block.hashCode()}"
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager?
        val flags = FULL_WAKE_LOCK or ACQUIRE_CAUSES_WAKEUP or ON_AFTER_RELEASE

        wakeLock = pm?.newWakeLock(flags, tag)?.apply {

            acquire(duration)
        }

    } catch (e: Throwable) {

        onError(e)
    }

    try {

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

fun Context.executeWithWorkManager(

    enabled: Boolean = BaseApplication.takeContext().canWorkManager(),
    onError: (e: Throwable) -> Unit = { e -> recordException(e) },
    block: () -> Unit

) {

    try {

        if (!enabled) {

            block()

            return
        }

        BackgroundTaskWorker.setTask(block)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<BackgroundTaskWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(constraints)
                .setInitialDelay(0, TimeUnit.MILLISECONDS)
                .build()

        val ctx = BaseApplication.takeContext()

        WorkManager.getInstance(ctx).enqueue(workRequest)

    } catch (e: Throwable) {

        onError(e)
    }
}

suspend fun <T> syncOnWorker(

    obtainable: Obtain<T?>,
    context: CoroutineContext = Dispatchers.IO

): T? = withContext(context) {

    val ctx = BaseApplication.takeContext()

    suspendCoroutine { continuation ->

        ctx.executeWithWorkManager {

            try {

                val result = obtainable.obtain()

                continuation.resume(result)

            } catch (e: Throwable) {

                continuation.resumeWithException(e)
            }
        }
    }
}