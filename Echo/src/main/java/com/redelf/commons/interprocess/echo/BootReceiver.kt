package com.redelf.commons.interprocess.echo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {

            context.applicationContext.startService(Intent(context, EchoService::class.java))
        }
    }
}