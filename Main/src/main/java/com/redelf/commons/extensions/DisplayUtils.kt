package com.redelf.commons.extensions

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display

/**
 * Display utilities for ATMOSphere Presenter application
 * Simplified version for Toolkit integration
 * Handles display detection and targeting
 */
object DisplayUtils {
    
    private const val TAG = "ATMOSphere :: ToolkitDisplayUtils"
    
    /**
     * Find the optimal display for Presenter application
     * Simplified version that works in Toolkit context
     */
    fun findOptimalDisplay(context: Context): Int {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displays = displayManager.displays
        
        Log.i(TAG, "Available displays: ${displays.size}")
        displays.forEach { display ->
            Log.i(TAG, "Display ${display.displayId}: ${display.width}x${display.height}")
        }
        
        // Step 1: Look for 4K displays (3840x2160 or 4096x2160)
        val fourKDisplay = find4KDisplay(displays)
        if (fourKDisplay != null) {
            Log.i(TAG, "Found 4K display: ${fourKDisplay.displayId} (${fourKDisplay.width}x${fourKDisplay.height})")
            return fourKDisplay.displayId
        }
        
        // Step 2: Look for HDMI displays specifically
        val hdmiDisplay = findHDMIDisplay(displays)
        if (hdmiDisplay != null) {
            Log.i(TAG, "Found HDMI display: ${hdmiDisplay.displayId} (${hdmiDisplay.width}x${hdmiDisplay.height})")
            return hdmiDisplay.displayId
        }
        
        // Step 3: Any external display with highest resolution
        val externalDisplays = displays.filter { it.displayId != Display.DEFAULT_DISPLAY }
        if (externalDisplays.isNotEmpty()) {
            val bestExternal = externalDisplays.maxByOrNull { it.width * it.height }
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
     * Find 4K display (3840x2160 or 4096x2160)
     */
    private fun find4KDisplay(displays: Array<Display>): Display? {
        return displays.firstOrNull { display ->
            (display.width == 3840 && display.height == 2160) ||
            (display.width == 4096 && display.height == 2160)
        }
    }
    
    /**
     * Find HDMI display by checking uniqueId
     */
    private fun findHDMIDisplay(displays: Array<Display>): Display? {
        // For Toolkit compatibility, we'll use display properties instead of uniqueId
        return displays.firstOrNull { display ->
            // Check if it's an external display (non-default)
            display.displayId != Display.DEFAULT_DISPLAY
        }
    }
    
    /**
     * Get display info for logging
     */
    fun getDisplayInfo(display: Display): String {
        return "Display ${display.displayId}: ${display.width}x${display.height}"
    }
}