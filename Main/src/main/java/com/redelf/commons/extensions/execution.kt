package com.redelf.commons.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP
import android.os.PowerManager.FULL_WAKE_LOCK
import android.os.PowerManager.ON_AFTER_RELEASE
import android.os.PowerManager.PARTIAL_WAKE_LOCK
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.execution.BackgroundTaskWorker
import com.redelf.commons.logging.Console
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicBoolean

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

    duration: Long = 30000L,
    onError: (e: Throwable) -> Unit = { e -> recordException(e) },
    block: () -> Unit

) {

    var wakeLock: PowerManager.WakeLock? = null

    try {

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

            wakeLock?.release()

        } catch (e: Throwable) {

            onError(e)
        }
    }
}

fun Context.executeWithWorkManager(

    onError: (e: Throwable) -> Unit = { e -> recordException(e) },
    block: () -> Unit

) {

    try {

        BackgroundTaskWorker.setTask(block)

        val workRequest =
            OneTimeWorkRequestBuilder<BackgroundTaskWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

        val ctx = BaseApplication.takeContext()

        WorkManager.getInstance(ctx).enqueue(workRequest)

    } catch (e: Throwable) {

        onError(e)
    }
}