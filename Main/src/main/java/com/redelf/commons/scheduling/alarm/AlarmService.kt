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