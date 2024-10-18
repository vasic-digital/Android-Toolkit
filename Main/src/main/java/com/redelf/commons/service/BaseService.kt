package com.redelf.commons.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager

abstract class BaseService : Service(), Serving {

    override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent? {

        receiver?.let { r ->

            filter?.let { f ->

                LocalBroadcastManager.getInstance(applicationContext).registerReceiver(r, f)
            }
        }

        return null
    }

    override fun unregisterReceiver(receiver: BroadcastReceiver?) {

        receiver?.let {

            LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(it)
        }
    }

    override fun sendBroadcast(intent: Intent?) {

        intent?.let {

            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(it)
        }
    }
}