package com.redelf.commons.interprocess

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.redelf.commons.logging.Console

class InterprocessReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        intent?.let {

            Console.log("Received intent :: ${it.action}")

            context?.let { ctx ->

                LocalBroadcastManager.getInstance(ctx.applicationContext).sendBroadcast(intent)
            }
        }
    }
}
