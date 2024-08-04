package com.redelf.commons.transmission.alarm

import android.content.Context
import android.content.Intent
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.scheduling.alarm.AlarmCallback
import com.redelf.commons.transmission.TransmissionService

class TransmissionAlarmCallback(private val ctx: Context) : AlarmCallback {

    private val alarmTag = "Alarm ::"

    override fun onAlarm(alarm: Int) {

        Console.log("$alarmTag Received: $alarm")

        when (alarm) {

            TransmissionService.BROADCAST_EXTRA_CODE -> {

                val serviceIntent = Intent(ctx, TransmissionService::class.java)

                try {

                    ctx.applicationContext.startService(serviceIntent)

                } catch (e: IllegalStateException) {

                    recordException(e)
                }
            }

            else -> {

                Console.error("$alarmTag Unknown alarm received: $alarm")
            }
        }
    }
}