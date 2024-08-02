package com.redelf.commons.transmission

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Binder
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.logging.Console
import com.redelf.commons.net.connectivity.Connectivity
import com.redelf.commons.scheduling.alarm.AlarmReceiver
import com.redelf.commons.scheduling.alarm.AlarmScheduler
import com.redelf.commons.service.BaseService
import com.redelf.commons.transmission.alarm.TransmissionAlarmCallback
import java.util.concurrent.atomic.AtomicBoolean

class TransmissionService : BaseService() {

    companion object {

        var DEBUG: Boolean? = null
        val BROADCAST_EXTRA_CODE: Int = BaseApplication.ALARM_SERVICE_JOB_ID_MIN.get() + 1
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

            Console.log("BROADCAST_ACTION_RESULT on receive")

            intent?.let {

                if (it.action == TransmissionManager.BROADCAST_ACTION_RESULT) {

                    Console.log("BROADCAST_ACTION_RESULT on action")

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

        Console.log("onStartCommand()")

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

        Console.log("onCreate()")
    }

    override fun onDestroy() {
        super.onDestroy()

        Console.log("onDestroy()")

        try {

            unregisterReceiver(connectivityListener)

        } catch (e: IllegalArgumentException) {

            Console.warning(e.message)
        }

        try {

            unregisterReceiver(resultsReceiver)

        } catch (e: IllegalArgumentException) {

            Console.warning(e.message)
        }
    }

    private fun send(ctx: Context, executedFrom: String = "") {

        Console.log("Send (service) :: executedFrom='$executedFrom'")

        val intent = Intent(TransmissionManager.BROADCAST_ACTION_SEND)
        ctx.sendBroadcast(intent)

        Console.log(

            "BROADCAST_ACTION_SEND on transmission service send(...)" +
                " executedFrom='$executedFrom'"
        )
    }

    private fun getAlarmInterval(): Long {

        if (DEBUG ?: BaseApplication.DEBUG.get()) {

            return System.currentTimeMillis() + (60 * 1000)
        }

        return System.currentTimeMillis() + (10 * 60 * 1000)
    }

    private fun scheduleAlarm(success: Boolean) {

        val tag = "Alarm :: Scheduling :: $success ::"

        Console.log("$tag Start")

        if (success) {

            unregisterAlarmCallback()

            AlarmScheduler(applicationContext).unSchedule(BROADCAST_EXTRA_CODE)

        } else {

            registerAlarmCallback()

            val time = getAlarmInterval()

            AlarmScheduler(applicationContext).schedule(BROADCAST_EXTRA_CODE, time)
        }

        Console.log("$tag End")
    }

    private fun registerAlarmCallback() {

        AlarmReceiver.register(alarmCallback)
    }

    private fun unregisterAlarmCallback() = AlarmReceiver.register(alarmCallback)

    inner class TransmissionServiceBinder : Binder() {

        fun getService(): TransmissionService = this@TransmissionService
    }
}