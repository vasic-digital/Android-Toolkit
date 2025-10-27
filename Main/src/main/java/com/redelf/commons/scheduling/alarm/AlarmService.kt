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

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import androidx.work.Configuration
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.logging.Console
import com.redelf.commons.scheduling.alarm.AlarmScheduler.Companion.ALARM_ACTION
import com.redelf.commons.scheduling.alarm.AlarmScheduler.Companion.ALARM_VALUE
import com.redelf.commons.service.Serving

class AlarmService : JobService(), Serving {

    init {

        val builder: Configuration.Builder = Configuration.Builder()

        builder.setJobSchedulerJobIdRange(

            BaseApplication.ALARM_SERVICE_JOB_ID_MIN.get(),
            BaseApplication.ALARM_SERVICE_JOB_ID_MAX.get()
        )
    }

    override fun onStartJob(params: JobParameters?): Boolean {

        Console.log("AlarmService :: Job started")

        val extras = params?.extras
        val what = extras?.getInt(ALARM_VALUE, -1) ?: -1

        if (what > -1) {

            val alarmIntent = Intent(applicationContext, AlarmReceiver::class.java)
            alarmIntent.action = ALARM_ACTION
            alarmIntent.putExtra(ALARM_VALUE, what)

            sendBroadcast(alarmIntent)
        }

        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {

        Console.log("AlarmService :: Job stopped")

        return false
    }
}