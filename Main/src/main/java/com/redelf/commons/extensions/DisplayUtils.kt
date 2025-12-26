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
     * Simplified version that works in Toolkit context
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
        
        // Step 1: Look for 4K displays (3840x2160 or 4096x2160)
        val fourKDisplay = find4KDisplay(displays)
        if (fourKDisplay != null) {
            val size = Point()
            fourKDisplay.getRealSize(size)
            Log.i(TAG, "Found 4K display: ${fourKDisplay.displayId} (${size.x}x${size.y})")
            return fourKDisplay.displayId
        }
        
        // Step 2: Look for HDMI displays specifically
        val hdmiDisplay = findHDMIDisplay(displays)
        if (hdmiDisplay != null) {
            val size = Point()
            hdmiDisplay.getRealSize(size)
            Log.i(TAG, "Found HDMI display: ${hdmiDisplay.displayId} (${size.x}x${size.y})")
            return hdmiDisplay.displayId
        }
        
        // Step 3: Any external display with highest resolution
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
     */
    private fun find4KDisplay(displays: Array<Display>): Display? {
        return displays.firstOrNull { display ->
            val size = Point()
            display.getRealSize(size)
            (size.x == 3840 && size.y == 2160) ||
            (size.x == 4096 && size.y == 2160)
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
        val size = Point()
        display.getRealSize(size)
        return "Display ${display.displayId}: ${size.x}x${size.y}"
    }
}