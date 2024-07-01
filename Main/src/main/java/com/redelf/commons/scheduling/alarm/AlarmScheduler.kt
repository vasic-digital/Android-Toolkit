package com.redelf.commons.scheduling.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.logging.Console
import com.redelf.commons.scheduling.Schedule

class AlarmScheduler(

    ctx: Context,
    private val logTag: String = "Alarm :: Scheduling ::"

) : Schedule<Int> {

    companion object {

        const val ALARM_VALUE = "AlarmScheduler.VALUE"
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

        unSchedule(what, "schedule")

        val tag = "$logTag ON ::"

        alarmIntent.putExtra(ALARM_VALUE, what)

        val pendingIntent = PendingIntent.getBroadcast(

            context,
            what,
            alarmIntent,
            flags
        )

        Console.log("$tag Scheduling new alarm: What=$what, toWhen=$toWhen")

        // FIXME:
        alarmManager.setExactAndAllowWhileIdle(

            AlarmManager.RTC_WAKEUP,
            toWhen,
            pendingIntent
        )

        Console.log("$tag COMPLETED")

        return true
    }

    fun unSchedule(what: Int, from: String = ""): Boolean {

        val tag = if (isEmpty(from)) {

            "$logTag OFF ::"

        } else {

            "$from :: $logTag OFF ::"
        }

        val pendingIntent = PendingIntent.getBroadcast(

            context,
            what,
            alarmIntent,
            flags
        )

        Console.log("$tag Cancelling scheduled alarm (if any)")

        alarmManager.cancel(pendingIntent)

        Console.log("$tag COMPLETED")

        return true
    }

    override fun unSchedule(what: Int): Boolean {

        return unSchedule(what, "unSchedule")
    }
}