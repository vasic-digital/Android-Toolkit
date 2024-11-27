package com.redelf.commons.application

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.redelf.commons.logging.Console

class OnClearFromRecentService : Service() {

    companion object {

        const val TAG = "On clear from recent service ::"
        const val ACTION = "com.redelf.commons.application.OnClearFromRecentService"
    }

    override fun onBind(intent: Intent?): IBinder? {

        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Console.log("$TAG Started")

        return START_NOT_STICKY
    }

    override fun onDestroy() {

        super.onDestroy()

        Console.log("$TAG Destroyed")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {

        Console.log("$TAG Task removed :: START")

        val intent = Intent(ACTION)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

        Console.log("$TAG Task removed :: END")

        stopSelf()
    }
}