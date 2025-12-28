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
     * Prioritizes HDMI2 (Display ID 2) for 4K monitor
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
        
        // Step 1: Check for ATMOSphere system property indicating target display
        val atmosphereTargetDisplay = System.getProperty("atmosphere.presenter.target_display")
        if (atmosphereTargetDisplay != null) {
            try {
                val displayId = atmosphereTargetDisplay.toInt()
                val targetDisplay = displays.firstOrNull { it.displayId == displayId }
                if (targetDisplay != null) {
                    Log.i(TAG, "Using ATMOSphere target display: $displayId")
                    return displayId
                }
            } catch (e: NumberFormatException) {
                Log.w(TAG, "Invalid atmosphere.presenter.target_display value: $atmosphereTargetDisplay")
            }
        }
        
        // Step 2: Prioritize Display ID 2 (HDMI2) as it should be the 4K monitor
        val hdmi2Display = displays.firstOrNull { it.displayId == 2 }
        if (hdmi2Display != null) {
            val size = Point()
            hdmi2Display.getRealSize(size)
            Log.i(TAG, "Using HDMI2 display (ID 2): ${size.x}x${size.y}")
            return hdmi2Display.displayId
        }
        
        // Step 3: Look for 4K displays (3840x2160 or 4096x2160)
        val fourKDisplay = find4KDisplay(displays)
        if (fourKDisplay != null) {
            val size = Point()
            fourKDisplay.getRealSize(size)
            Log.i(TAG, "Found 4K display: ${fourKDisplay.displayId} (${size.x}x${size.y})")
            return fourKDisplay.displayId
        }
        
        // Step 4: Look for HDMI displays specifically
        val hdmiDisplay = findHDMIDisplay(displays)
        if (hdmiDisplay != null) {
            val size = Point()
            hdmiDisplay.getRealSize(size)
            Log.i(TAG, "Found HDMI display: ${hdmiDisplay.displayId} (${size.x}x${size.y})")
            return hdmiDisplay.displayId
        }
        
        // Step 5: Any external display with highest resolution
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
     * Find HDMI display by checking uniqueId and properties
     */
    private fun findHDMIDisplay(displays: Array<Display>): Display? {
        // For Toolkit compatibility, we'll use display properties instead of uniqueId
        return displays.firstOrNull { display ->
            // Check if it's an external display (non-default)
            display.displayId != Display.DEFAULT_DISPLAY &&
            // Prefer displays with higher IDs (typically HDMI2 which should be 4K monitor)
            display.displayId >= 2
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