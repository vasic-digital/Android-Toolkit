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


package com.redelf.commons.extensions

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.view.Display
import com.redelf.commons.logging.Console

const val EXTRA_DISPLAY_ID = "Extra.Display.ID"

fun Context.startActivityOnExtendedDisplay(what: Class<*>, from: String? = this::class.simpleName) {

    var displayId = 10
    var targetDisplay: Display? = null
    val tag = "ATMOSphere :: Launching on external display :: From = '$from' ::"
    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val displays = displayManager.displays

    Console.debug("$tag Looking for the display")

    while (displayId > 0 && targetDisplay == null) {

        targetDisplay = displays.firstOrNull { it.displayId == displayId }

        if (targetDisplay == null) {

            displayId--
        }
    }

    targetDisplay?.let {

        val intent = Intent(this, what)
        intent.putExtra(EXTRA_DISPLAY_ID, displayId)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId)

        try {

            startActivity(intent, options.toBundle())

            Console.info("$tag Display found :: ID = $displayId")

        } catch (e: Throwable) {

            recordException(e)
        }

        if (this is Activity && !isFinishing) {

            finish()
        }
    }

    if (targetDisplay == null) {

        Console.error("$tag No target display to present")

        if (this is Activity && !isFinishing) {

            Console.log("$tag Finishing")

            finish()
        }
    }
}

fun Context.isInForeground(): Boolean {

    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
    val runningAppProcesses = activityManager?.runningAppProcesses ?: return false

    return runningAppProcesses.any { process ->

        process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                process.processName == packageName
    }
}

fun Context.isInBackground(): Boolean = !isInForeground()