/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


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