package com.redelf.commons.interprocess

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

import androidx.work.Worker
import androidx.work.WorkerParameters
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.yieldWhile
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.OnObtain
import java.util.concurrent.atomic.AtomicBoolean

abstract class InterprocessWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    protected abstract val tag: String

    protected lateinit var actions: List<String>

    private val ready = AtomicBoolean()

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            intent?.let {

                onIntentReceived(it)
            }
        }
    }

    init {

        val filter = IntentFilter()

        actions.forEach {

            filter.addAction(it)
        }

        applicationContext.registerReceiver(broadcastReceiver, filter)

        ready.set(true)
    }

    open fun isReady(): Boolean = ready.get()

    override fun onStopped() {
        super.onStopped()

        applicationContext.unregisterReceiver(broadcastReceiver)
    }

    fun sendMessage(action: String, callback: OnObtain<Boolean>? = null) {

        val intent = Intent(action)

        sendMessage(intent, callback)
    }

    open fun sendMessage(intent: Intent, callback: OnObtain<Boolean>? = null) {

        exec(

            onRejected = { err ->

                callback?.onFailure(err)

                if (callback == null) {

                    Console.error("$tag Failed to send message :: ${err.message}")
                }
            }

        ) {

            yieldWhile { !isReady() }

            applicationContext.sendBroadcast(intent)

            callback?.onCompleted(true)
        }
    }

    protected open fun hello() {

        Console.log("$tag Hello")
    }

    protected abstract fun onIntent(intent: Intent)

    private fun onIntentReceived(intent: Intent) {

        Console.log("$tag Received intent :: ${intent.action}")

        onIntent(intent)
    }
}