/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.redelf.commons.scheduling.alarm

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import com.redelf.commons.application.BaseApplication
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

    override fun schedule(

        what: Int,
        toWhen: Long

    ): Boolean {

        val condition = what < BaseApplication.ALARM_SERVICE_JOB_ID_MIN.get() ||
                what > BaseApplication.ALARM_SERVICE_JOB_ID_MAX.get()

        if (condition) {

            Console.error(

                "$logTag :: Cannot schedule, the Job ID is out of " +
                    "expected boundaries (${BaseApplication.ALARM_SERVICE_JOB_ID_MIN} - " +
                        "${BaseApplication.ALARM_SERVICE_JOB_ID_MAX})"
            )

            return false
        }

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