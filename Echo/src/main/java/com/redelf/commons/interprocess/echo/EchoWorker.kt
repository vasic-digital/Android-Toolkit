package com.redelf.commons.interprocess.echo

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.WorkerParameters
import com.redelf.commons.interprocess.InterprocessWorker

class EchoWorker(ctx: Context, params: WorkerParameters) : InterprocessWorker(ctx, params) {

    init {

        actions = listOf("com.redelf.commons.interprocess.echo")
    }

    override fun onIntent(intent: Intent) {

        val message = intent.getStringExtra("data")
        val responseIntent = Intent(actions.first())
        responseIntent.putExtra("data", "Echo :: $message")
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(responseIntent)
    }

    override fun doWork(): Result {

        return Result.success()
    }
}