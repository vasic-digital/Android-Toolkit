package com.redelf.commons.extensions

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.view.Display
import com.redelf.commons.logging.Console


const val EXTRA_DISPLAY_ID = "Extra.Display.ID"

/**
 * ATMOSphere Enhanced Display Extension
 * Replaces hardcoded Display 4 logic with intelligent display detection
 * Ensures Presenter opens on HDMI display (4K monitor) instead of touch screen
 */
fun Context.startActivityOnExtendedDisplay(what: Class<*>, from: String? = this::class.simpleName) {

    val tag = "ATMOSphere::startActivityOnExtendedDisplay"
    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val displays = displayManager.displays
    
    Console.debug("$tag Starting activity: ${what.simpleName}")
    Console.debug("$tag Available displays: ${displays.size}")
    
    // Use DisplayUtils for intelligent display selection
    val targetDisplayId = DisplayUtils.findOptimalDisplay(this)
    val targetDisplay = displays.firstOrNull { it.displayId == targetDisplayId }
    
    if (targetDisplay != null) {
        Console.info("$tag Found optimal display: ${DisplayUtils.getDisplayInfo(targetDisplay)}")
        
        val intent = Intent(this, what)
        intent.putExtra(EXTRA_DISPLAY_ID, targetDisplayId)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(targetDisplayId)

        try {
            startActivity(intent, options.toBundle())
            Console.info("$tag Successfully launched on display $targetDisplayId")
        } catch (e: Exception) {
            Console.error("$tag Failed to launch on display $targetDisplayId: ${e.message}")
            recordException(e)
        }

        if (this is Activity && !isFinishing) {
            Console.debug("$tag Finishing caller activity")
            finish()
        }
    } else {
        Console.error("$tag No suitable display found for presentation")
        
        if (this is Activity && !isFinishing) {
            Console.debug("$tag Finishing due to no suitable display")
            finish()
        }
    }
}