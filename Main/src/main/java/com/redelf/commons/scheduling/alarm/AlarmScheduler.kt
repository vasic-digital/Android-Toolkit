package com.redelf.commons.scheduling.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
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

    private val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(

        what: Int,
        toWhen: Long

    ): Boolean {

        unSchedule(what, "schedule")

        val tag = "$logTag ON ::"

        val delay = toWhen - System.currentTimeMillis()

        Console.log(

            "$tag Scheduling new alarm: what = $what, to when = " +
                    "$toWhen (delay = $delay ms)"
        )

        val componentName = ComponentName(context, AlarmService::class.java)

        val extras = PersistableBundle()

        extras.putInt(ALARM_VALUE, what)

        val jobInfo = JobInfo.Builder(what, componentName)
            .setMinimumLatency(delay)
            .setOverrideDeadline(1000)
            .setExtras(extras)
            .build()

        val jobScheduler = context?.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler?
        jobScheduler?.schedule(jobInfo)

        Console.log("$tag COMPLETED")

        return true
    }

    private fun unSchedule(what: Int, from: String = ""): Boolean {

        val tag = if (isEmpty(from)) {

            "$logTag OFF ::"

        } else {

            "$from :: $logTag OFF ::"
        }

        Console.log("$tag Cancelling scheduled alarm (if any)")

        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler?

        jobScheduler?.cancel(what)

        Console.log("$tag COMPLETED")

        return true
    }

    override fun unSchedule(what: Int): Boolean {

        return unSchedule(what, "unSchedule")
    }
}