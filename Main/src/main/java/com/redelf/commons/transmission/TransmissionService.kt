package com.redelf.commons.transmission

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Binder
import com.redelf.commons.application.BaseApplication

import com.redelf.commons.connectivity.Connectivity
import com.redelf.commons.scheduling.alarm.AlarmReceiver
import com.redelf.commons.scheduling.alarm.AlarmScheduler
import com.redelf.commons.service.BaseService
import com.redelf.commons.transmission.alarm.TransmissionAlarmCallback
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class TransmissionService : BaseService() {

    companion object {

        var DEBUG = BaseApplication.DEBUG.get()

        const val BROADCAST_EXTRA_CODE = 1
    }

    private val binder = TransmissionServiceBinder()
    private val connectivityListenerReady = AtomicBoolean()
    private val connectivityListenerState = AtomicBoolean(true)

    private lateinit var alarmCallback: TransmissionAlarmCallback

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
        registerReceiver(connectivityListener, connectivityIntentFilter)

        val resultsIntentFilter = IntentFilter(TransmissionManager.BROADCAST_ACTION_RESULT)
        registerReceiver(resultsReceiver, resultsIntentFilter)

        send(this, "onStartCommand")

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        alarmCallback = TransmissionAlarmCallback(applicationContext)

        Timber.v("onCreate()")
    }

    override fun onDestroy() {
        super.onDestroy()

        Timber.v("onDestroy()")

        try {

            unregisterReceiver(connectivityListener)

        } catch (e: IllegalArgumentException) {

            Timber.w(e.message)
        }

        try {

            unregisterReceiver(resultsReceiver)

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

        if (DEBUG) {

            return System.currentTimeMillis() + (60 * 1000)
        }

        return System.currentTimeMillis() + (10 * 60 * 1000)
    }

    private fun scheduleAlarm(success: Boolean) {

        val tag = "Alarm :: Scheduling :: $success ::"

        Timber.v("$tag Start")

        if (success) {

            unregisterAlarmCallback()

            AlarmScheduler(applicationContext).unSchedule(BROADCAST_EXTRA_CODE)

        } else {

            registerAlarmCallback()

            val time = getAlarmInterval()
            AlarmScheduler(applicationContext).schedule(BROADCAST_EXTRA_CODE, time)
        }

        Timber.v("$tag End")
    }

    private fun registerAlarmCallback() {

        AlarmReceiver.register(alarmCallback)
    }

    private fun unregisterAlarmCallback() = AlarmReceiver.register(alarmCallback)

    inner class TransmissionServiceBinder : Binder() {

        fun getService(): TransmissionService = this@TransmissionService
    }
}