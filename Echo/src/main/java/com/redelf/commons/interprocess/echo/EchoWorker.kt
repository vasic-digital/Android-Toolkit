package com.redelf.commons.interprocess.echo

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.WorkerParameters
import com.redelf.commons.interprocess.InterprocessWorker
import com.redelf.commons.logging.Console

class EchoWorker(ctx: Context, params: WorkerParameters) : InterprocessWorker(ctx, params) {

    private val tag = "Echo ::"

    init {

        actions = listOf("com.redelf.commons.interprocess.echo")
    }

    override fun onIntent(intent: Intent) {

        val message = intent.getStringExtra("data")

        Console.log("Received echo request :: $message")

        val responseIntent = Intent(actions.first())
        responseIntent.putExtra("data", "Echo :: $message")
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(responseIntent)

        Console.log("Sent echo response :: $message")
    }

    override fun doWork(): Result {

        return Result.success()
    }
}