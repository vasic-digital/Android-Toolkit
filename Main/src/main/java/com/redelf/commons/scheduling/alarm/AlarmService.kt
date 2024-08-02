package com.redelf.commons.scheduling.alarm

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import com.redelf.commons.logging.Console
import androidx.work.Configuration
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.scheduling.alarm.AlarmScheduler.Companion.ALARM_ACTION
import com.redelf.commons.scheduling.alarm.AlarmScheduler.Companion.ALARM_VALUE

class AlarmService : JobService() {

    init {

        val builder: Configuration.Builder = Configuration.Builder()

        builder.setJobSchedulerJobIdRange(

            BaseApplication.ALARM_SERVICE_JOB_ID_MIN,
            BaseApplication.ALARM_SERVICE_JOB_ID_MAX
        )
    }

    override fun onStartJob(params: JobParameters?): Boolean {

        Console.log("AlarmService :: Job started")

        val extras = params?.extras
        val what = extras?.getInt(ALARM_VALUE, -1) ?: -1

        if (what > -1) {

            val alarmIntent = Intent(applicationContext, AlarmReceiver::class.java)
            alarmIntent.action = ALARM_ACTION

            sendBroadcast(alarmIntent)
        }

        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {

        Console.log("AlarmService :: Job stopped")

        return false
    }
}