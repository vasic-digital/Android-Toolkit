package com.redelf.commons.execution

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.redelf.commons.extensions.recordException

class BackgroundTaskWorker(

    context: Context,
    params: WorkerParameters

) : CoroutineWorker(context, params) {

    companion object {

        var customCodeBlock: (suspend () -> Unit)? = null

        fun setTask(what: suspend () -> Unit) {

            customCodeBlock = what
        }
    }

    override suspend fun doWork(): Result {

        return try {

            executeCustomTask()

            Result.success()

        } catch (e: Throwable) {

            recordException(e)

            Result.failure()
        }
    }

    private suspend fun executeCustomTask() {

        customCodeBlock?.invoke()
    }
}