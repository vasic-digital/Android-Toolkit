package com.redelf.commons.interprocess

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class InterprocessService : Service() {

    private val tag = "Interprocess service ::"

    private val binder = InterprocessBinder()

    override fun onBind(intent: Intent?): IBinder {

        return binder
    }

    inner class InterprocessBinder : Binder() {

        fun getService(): InterprocessService = this@InterprocessService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_STICKY
    }

    fun onIntent(intent: Intent) {

        // TODO: Connect with registered parties
    }
}