package com.redelf.commons.scheduling.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.redelf.commons.scheduling.Schedule
import timber.log.Timber

class AlarmScheduler(

    ctx: Context,
    private val logTag: String = "Alarm :: Scheduling ::"

) : Schedule<Int> {

    companion object {

        const val ALARM_ACTION = "AlarmScheduler.ON_ALARM"
    }

    private val context = ctx.applicationContext

    private val alarmIntent = Intent(context, AlarmReceiver::class.java)

    private val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {

        alarmIntent.action = ALARM_ACTION
    }
    override fun schedule(

        what: Int,
        toWhen: Long

    ): Boolean {

        val tag = "$logTag ON ::"

        val pendingIntent = PendingIntent.getBroadcast(

            context,
            what,
            alarmIntent,
            flags
        )

        unSchedule(what)

        Timber.v("$tag Scheduling new alarm: What=$what, toWhen=$toWhen")

        alarmManager.setExactAndAllowWhileIdle(

            AlarmManager.RTC_WAKEUP,
            toWhen,
            pendingIntent
        )

        Timber.v("$tag COMPLETED")

        return true
    }

    override fun unSchedule(what: Int): Boolean {

        val tag = "$logTag OFF ::"

        val pendingIntent = PendingIntent.getBroadcast(

            context,
            what,
            alarmIntent,
            flags
        )

        Timber.v("$tag Cancelling scheduled alarm (if any)")

        alarmManager.cancel(pendingIntent)

        Timber.v("$tag COMPLETED")

        return true
    }
}