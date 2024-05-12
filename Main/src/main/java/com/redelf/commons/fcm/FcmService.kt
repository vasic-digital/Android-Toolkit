package com.redelf.commons.fcm

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

open class FcmService : FirebaseMessagingService() {

    companion object {

        const val BROADCAST_KEY_TOKEN = "key.token"
        const val BROADCAST_ACTION_TOKEN = "action.token"
        const val BROADCAST_ACTION_EVENT = "action.event"
    }

    override fun onCreate() {
        super.onCreate()

        Timber.v("onCreate()")
    }

    override fun onDestroy() {
        super.onDestroy()

        Timber.v("onDestroy()")
    }

    override fun onLowMemory() {
        super.onLowMemory()

        Timber.e("onLowMemory()")
    }

    override fun onNewToken(token: String) {

        super.onNewToken(token)

        Timber.i("New token available: $token")

        val intent = Intent(BROADCAST_ACTION_TOKEN)
        intent.putExtra(BROADCAST_KEY_TOKEN, token)
        sendBroadcast(intent)
    }

    override fun onMessageReceived(message: RemoteMessage) {

        super.onMessageReceived(message)

        val data = message.data

        Timber.i("New FCM message received: $data")

        wakeUpScreen()

        val intent = Intent(BROADCAST_ACTION_EVENT)

        data.forEach { (key, value) ->

            intent.putExtra(key, value)
        }

        sendBroadcast(intent)
    }

    @Suppress("DEPRECATION")
    private fun wakeUpScreen() {

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val isScreenOn = powerManager.isInteractive

        if (!isScreenOn) {

            val tag = "WakeLock:1"

            val wl = powerManager.newWakeLock(

                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                tag
            )

            wl.acquire(2000)

            val wlCpu = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag)
            wlCpu.acquire(2000)
        }
    }

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