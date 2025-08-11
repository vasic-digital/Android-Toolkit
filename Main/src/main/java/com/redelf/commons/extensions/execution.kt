package com.redelf.commons.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP
import android.os.PowerManager.PARTIAL_WAKE_LOCK
import com.redelf.commons.logging.Console
import net.bytebuddy.implementation.bytecode.Throw
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
fun Context.executeWithWakeLock(duration: Long = 30000L, block: () -> Unit) {

    var wakeLock: PowerManager.WakeLock? = null

    try {

        val tag = "WakeLockExecute.${block.hashCode()}"
        val flags = PARTIAL_WAKE_LOCK or ACQUIRE_CAUSES_WAKEUP
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager?

        wakeLock = pm?.newWakeLock(flags, tag)?.apply {

            acquire(duration)
        }

    } catch (e: Throwable) {

        recordException(e)
    }

    try {

        block()

    } finally {

        try {

            wakeLock?.release()

        } catch (e: Throwable) {

            recordException(e)
        }
    }
}