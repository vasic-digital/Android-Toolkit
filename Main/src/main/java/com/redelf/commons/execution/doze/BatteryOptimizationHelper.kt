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