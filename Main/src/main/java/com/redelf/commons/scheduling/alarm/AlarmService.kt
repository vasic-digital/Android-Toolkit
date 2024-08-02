package com.redelf.commons.scheduling.alarm

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import com.redelf.commons.logging.Console
import com.redelf.commons.scheduling.alarm.AlarmScheduler.Companion.ALARM_ACTION
import com.redelf.commons.scheduling.alarm.AlarmScheduler.Companion.ALARM_VALUE

class AlarmService : JobService() {

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