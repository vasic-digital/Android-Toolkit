package com.redelf.commons.interprocess.echo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {

            val echoWorkerRequest = OneTimeWorkRequest.Builder(EchoWorker::class.java).build()
            WorkManager.getInstance(context).enqueue(echoWorkerRequest)
        }
    }
}