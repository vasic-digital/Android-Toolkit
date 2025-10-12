package com.redelf.commons.media.player

import android.content.Context
import android.os.PowerManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.redelf.commons.execution.doze.DozeModeIOException
import com.redelf.commons.logging.Console

class ExoPlayerWorker (

    context: Context,
    params: WorkerParameters,


) : CoroutineWorker(context, params) {

    companion object {

        var action: (suspend () -> Unit)? = null
    }

    override suspend fun doWork(): Result {

        acquireWakeLock()

        try {

            executeCustomTask()

            return Result.success()

        } catch (e: DozeModeIOException) {

            Console.error(e.message ?: "Error: ${e.javaClass.name}")

            // Schedule retry when device wakes from Doze
            return Result.retry()

        } catch (e: Exception) {

            Console.error(e.message ?: "Error: ${e.javaClass.name}")

            return Result.failure()
        }
    }

    private fun acquireWakeLock(): PowerManager.WakeLock? {

        return try {

            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager

            val wakeLock = powerManager.newWakeLock(

                PowerManager.PARTIAL_WAKE_LOCK,
                "ExoPlayerWorker::WakeLock"
            )

            wakeLock.acquire(10 * 60 * 1000L) // 10 minutes

            wakeLock

        } catch (e: Exception) {

            Console.error(e.message ?: "Error: ${e.javaClass.name}")

            null
        }
    }

    private suspend fun executeCustomTask() {

        action?.invoke()
    }
}