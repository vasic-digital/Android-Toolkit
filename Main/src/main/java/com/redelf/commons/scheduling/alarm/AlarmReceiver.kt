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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.logging.Console
import com.redelf.commons.registration.Registration

class AlarmReceiver : BroadcastReceiver() {

    companion object : Registration<AlarmCallback> {

        private val callbacks: Callbacks<AlarmCallback> = Callbacks("AlarmReceiver")

        override fun register(subscriber: AlarmCallback) {

            callbacks.register(subscriber)
        }

        override fun unregister(subscriber: AlarmCallback) {

            callbacks.unregister(subscriber)
        }

        override fun isRegistered(subscriber: AlarmCallback): Boolean {

            return callbacks.isRegistered(subscriber)
        }

        private fun onAlarmReceived(value: Int) {

            callbacks.doOnAll(

                operation = object : CallbackOperation<AlarmCallback> {

                    override fun perform(callback: AlarmCallback) {

                        callback.onAlarm(value)
                    }
                },

                operationName = "onAlarmReceived: $value"
            )
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {

        Console.log("Alarm received: $intent")

        intent?.let {

            when (intent.action) {

                AlarmScheduler.ALARM_ACTION -> {

                    val alarmValue = intent.getIntExtra(AlarmScheduler.ALARM_VALUE, -1)
                    onAlarmReceived(alarmValue)
                }
            }
        }
    }
}
