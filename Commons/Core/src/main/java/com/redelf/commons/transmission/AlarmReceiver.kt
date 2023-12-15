package com.redelf.commons.transmission

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.redelf.commons.recordException
import timber.log.Timber

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        Timber.v("Alarm received")

        context?.let { ctx ->

            val serviceIntent = Intent(ctx, TransmissionService::class.java)

            try {

                ctx.applicationContext.startService(serviceIntent)

            } catch (e: IllegalStateException) {

                recordException(e)
            }
            return
        }
    }
}