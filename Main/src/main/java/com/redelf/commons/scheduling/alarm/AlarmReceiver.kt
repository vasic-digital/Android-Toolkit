package com.redelf.commons.scheduling.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class AlarmReceiver(private val callback: AlarmCallback) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        Timber.v("Alarm received: $intent")

        // TODO: Get extra from intent and pass to callback
        callback.onAlarm(-1)
    }
}
