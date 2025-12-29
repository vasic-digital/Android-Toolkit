package com.redelf.commons.extensions

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import android.view.Display.Mode

/**
 * Display utilities for ATMOSphere Presenter application
 * Simplified version for Toolkit integration
 * Handles display detection and targeting
 */
object DisplayUtils {
    
    private const val TAG = "ATMOSphere :: ToolkitDisplayUtils"
    
    /**
     * Find the optimal display for Presenter application
     * Fully dynamic detection - finds highest resolution external display
     * Handles display ID changes when HDMI is unplugged/plugged back in
     */
    fun findOptimalDisplay(context: Context): Int {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displays = displayManager.displays

        Log.i(TAG, "Available displays: ${displays.size}")
        displays.forEach { display ->
            val size = Point()
            display.getRealSize(size)
            Log.i(TAG, "Display ${display.displayId}: ${size.x}x${size.y}")
        }

        // Priority 1: Look for 4K displays (3840x2160 or 4096x2160)
        val fourKDisplay = find4KDisplay(displays)
        if (fourKDisplay != null) {
            val size = Point()
            fourKDisplay.getRealSize(size)
            Log.i(TAG, "Found 4K display: ${fourKDisplay.displayId} (${size.x}x${size.y})")
            return fourKDisplay.displayId
        }

        // Priority 2: Look for any external HDMI display
        val hdmiDisplay = findHDMIDisplay(displays)
        if (hdmiDisplay != null) {
            val size = Point()
            hdmiDisplay.getRealSize(size)
            Log.i(TAG, "Found HDMI display: ${hdmiDisplay.displayId} (${size.x}x${size.y})")
            return hdmiDisplay.displayId
        }

        // Priority 3: Any external display with highest resolution
        val externalDisplays = displays.filter { it.displayId != Display.DEFAULT_DISPLAY }
        if (externalDisplays.isNotEmpty()) {
            val bestExternal = externalDisplays.maxByOrNull { display ->
                val size = Point()
                display.getRealSize(size)
                size.x * size.y
            }
            if (bestExternal != null) {
                Log.i(TAG, "Using best external display: ${bestExternal.displayId}")
                return bestExternal.displayId
            }
        }
        
        // Fallback: Return default display
        Log.i(TAG, "Using default display")
        return Display.DEFAULT_DISPLAY
    }
    
/**
     * Find 4K displays (3840x2160 or 4096x2160)
     * Also consider near-4K resolutions that might be scaled
     */
    private fun find4KDisplay(displays: Array<Display>): Display? {
        return displays.firstOrNull { display ->
            val size = Point()
            display.getRealSize(size)
            // Exact 4K resolutions
            (size.x == 3840 && size.y == 2160) ||
            (size.x == 4096 && size.y == 2160) ||
            // Near 4K (scaled or slightly different)
            (size.x >= 3800 && size.x <= 4100 && size.y >= 2100 && size.y <= 2200)
        }
    }
    
    /**
     * Find any external HDMI display (non-default display)
     */
    private fun findHDMIDisplay(displays: Array<Display>): Display? {
        // Find any external display - ID can vary when unplugged/plugged
        return displays.firstOrNull { display ->
            display.displayId != Display.DEFAULT_DISPLAY
        }
    }
    
    /**
     * Get display info for logging
     */
    fun getDisplayInfo(display: Display): String {
        val size = Point()
        display.getRealSize(size)
        return "Display ${display.displayId}: ${size.x}x${size.y}"
    }
}