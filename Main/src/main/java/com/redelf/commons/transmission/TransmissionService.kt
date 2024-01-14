package com.redelf.commons.transmission

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.redelf.commons.BuildConfig
import com.redelf.commons.connectivity.Connectivity
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class TransmissionService : Service() {

    private val alarmRequestCode = 1
    private val binder = TransmissionServiceBinder()
    private val connectivityListenerReady = AtomicBoolean()
    private val connectivityListenerState = AtomicBoolean(true)

    private val connectivityListener = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            if (!connectivityListenerReady.get()) {

                connectivityListenerReady.set(true)
                return
            }

            context?.let {

                val connectivity = Connectivity()
                val connected = connectivity.isNetworkAvailable(it)

                if (connected && !connectivityListenerState.get()) {

                    send(it, "Network status change")
                }

                connectivityListenerState.set(connected)

                return
            }
        }
    }

    private val resultsReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            Timber.v("BROADCAST_ACTION_RESULT on receive")

            intent?.let {

                if (it.action == TransmissionManager.BROADCAST_ACTION_RESULT) {

                    Timber.v("BROADCAST_ACTION_RESULT on action")

                    val key = TransmissionManager.BROADCAST_EXTRA_RESULT
                    val result = it.getBooleanExtra(key, true)

                    scheduleAlarm(result)
                }
            }
        }
    }

    override fun onBind(intent: Intent?) = binder

    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Timber.v("onStartCommand()")

        val connectivityIntentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        LocalBroadcastManager.getInstance(this).registerReceiver(connectivityListener, connectivityIntentFilter)

        val resultsIntentFilter = IntentFilter(TransmissionManager.BROADCAST_ACTION_RESULT)
        LocalBroadcastManager.getInstance(this).registerReceiver(resultsReceiver, resultsIntentFilter)

        send(this, "onStartCommand")

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        Timber.v("onCreate()")
    }

    override fun onDestroy() {
        super.onDestroy()

        Timber.v("onDestroy()")

        try {

            LocalBroadcastManager.getInstance(this).unregisterReceiver(connectivityListener)

        } catch (e: IllegalArgumentException) {

            Timber.w(e.message)
        }

        try {

            LocalBroadcastManager.getInstance(this).unregisterReceiver(resultsReceiver)

        } catch (e: IllegalArgumentException) {

            Timber.w(e.message)
        }
    }

    private fun send(ctx: Context, executedFrom: String = "") {

        Timber.v("Send (service) :: executedFrom='$executedFrom'")

        val intent = Intent(TransmissionManager.BROADCAST_ACTION_SEND)
        ctx.sendBroadcast(intent)

        Timber.v(

            "BROADCAST_ACTION_SEND on transmission service send(...)" +
                " executedFrom='$executedFrom'"
        )
    }

    private fun getAlarmInterval(): Long {

        if (BuildConfig.DEBUG) {

            return System.currentTimeMillis() + (60 * 1000)
        }
        return System.currentTimeMillis() + (10 * 60 * 1000)
    }

    private fun scheduleAlarm(success: Boolean) {

        val tag = "scheduleAlarm() :: success=$success ::"
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(applicationContext, AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(

            applicationContext,
            alarmRequestCode,
            alarmIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        Timber.v("$tag Cancelling scheduled alarm (if any)")

        alarmManager.cancel(pendingIntent)

        if (!success) {

            Timber.w("$tag Scheduling new alarm")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    getAlarmInterval(),
                    pendingIntent
                )

            } else {

                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    getAlarmInterval(),
                    pendingIntent
                )
            }
        }
    }

    inner class TransmissionServiceBinder : Binder() {

        fun getService(): TransmissionService = this@TransmissionService
    }
}