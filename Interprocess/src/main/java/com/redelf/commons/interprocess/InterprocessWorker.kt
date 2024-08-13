package com.redelf.commons.interprocess

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.redelf.commons.logging.Console

abstract class InterprocessWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    protected abstract val tag: String

    protected lateinit var actions: List<String>

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            intent?.let {

                onIntentReceived(it)
            }
        }
    }

    init {

        actions.forEach {

            LocalBroadcastManager.getInstance(ctx).registerReceiver(

                broadcastReceiver,
                IntentFilter(it)
            )
        }
    }

    override fun onStopped() {
        super.onStopped()

        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(broadcastReceiver)
    }

    protected open fun hello() {

        Console.log("$tag Hello")
    }

    protected abstract fun onIntent(intent: Intent)

    private fun onIntentReceived(intent: Intent) {

        Console.log("$tag Received intent: ${intent.action}")

        onIntent(intent)
    }
}