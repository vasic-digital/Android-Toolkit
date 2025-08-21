package com.redelf.commons.extensions

import android.app.ActivityManager
import android.content.Context

fun Context.isInForeground(): Boolean {

    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
    val runningAppProcesses = activityManager?.runningAppProcesses ?: return false

    return runningAppProcesses.any { process ->

        process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                process.processName == packageName
    }
}

fun Context.isInBackground(): Boolean = !isInForeground()