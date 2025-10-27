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


package com.redelf.commons.execution.doze

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri
import com.redelf.commons.extensions.toast

object BatteryOptimizationHelper {

    /**
     * Check if app is ignoring battery optimizations
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    /**
     * Check if battery optimization can be requested
     * (Some manufacturers disable this functionality)
     */
    @SuppressLint("BatteryLife")
    fun canRequestBatteryOptimization(context: Context): Boolean {

        // Check if the intent can be handled
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {

            data = "package:${context.packageName}".toUri()
        }

        return intent.resolveActivity(context.packageManager) != null
    }

    /**
     * Request to ignore battery optimizations
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(activity: Activity, requestCode: Int = 1001) {

        try {

            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {

                data = "package:${activity.packageName}".toUri()
            }

            activity.startActivityForResult(intent, requestCode)

        } catch (_: ActivityNotFoundException) {

            // Some devices don't support this intent
            showUnsupportedDeviceMessage(activity)

        } catch (_: SecurityException) {

            // Handle security exception
            showPermissionDeniedMessage(activity)
        }
    }

    /**
     * Open battery optimization settings directly
     */
    fun openBatteryOptimizationSettings(context: Context) {

        try {

            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

        } catch (_: ActivityNotFoundException) {

            // Fallback to app info settings
            openAppInfoSettings(context)
        }
    }

    /**
     * Fallback to app info settings
     */
    private fun openAppInfoSettings(context: Context) {

        try {

            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {

                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

        } catch (_: Throwable) {

            context.toast("Please disable battery optimization for this app manually in settings")
        }
    }

    private fun showUnsupportedDeviceMessage(context: Context) {

        AlertDialog.Builder(context)
            .setTitle("Battery Optimization")
            .setMessage(

                "Your device doesn't support automatic battery optimization management. " +
                        "Please disable battery optimization for this app manually in settings."
            )
            .setPositiveButton("Open Settings") { _, _ ->

                openBatteryOptimizationSettings(context)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPermissionDeniedMessage(context: Context) {

        context.toast("Cannot manage battery optimization settings")
    }
}