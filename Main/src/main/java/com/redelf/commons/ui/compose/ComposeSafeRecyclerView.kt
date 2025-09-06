package com.redelf.commons.ui.compose

import android.content.Context
import android.util.AttributeSet
import com.redelf.commons.ui.SafeRecyclerView

/**
 * ComposeSafeRecyclerView is a drop-in replacement for SafeRecyclerView that extends 
 * ComposeRecyclerView and maintains SafeRecyclerView API compatibility.
 * 
 * This provides the same safety mechanisms as SafeRecyclerView while using 
 * Jetpack Compose internally for improved performance and thread safety.
 */
class ComposeSafeRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ComposeRecyclerView(context, attrs, defStyleAttr) {
    
    // All SafeRecyclerView functionality is inherited from ComposeRecyclerView
    // which already provides thread-safe operations and error handling
    
    /**
     * Override adapter setter to be extra safe
     */
    override var adapter: androidx.recyclerview.widget.RecyclerView.Adapter<*>?
        get() = super.adapter
        set(value) {
            try {
                super.adapter = value
            } catch (e: Exception) {
                // Log and continue - don't crash the app
                android.util.Log.e("ComposeSafeRecyclerView", "Error setting adapter", e)
            }
        }
    
    
    /**
     * Safe scroll to position that handles edge cases
     */
    override fun scrollToPosition(position: Int) {
        try {
            if (position >= 0 && position < (adapter?.itemCount ?: 0)) {
                super.scrollToPosition(position)
            }
        } catch (e: Exception) {
            // Log and continue - don't crash the app
            android.util.Log.e("ComposeSafeRecyclerView", "Error scrolling to position $position", e)
        }
    }
    
    /**
     * Safe smooth scroll that handles edge cases
     */
    override fun smoothScrollToPosition(position: Int) {
        try {
            if (position >= 0 && position < (adapter?.itemCount ?: 0)) {
                super.smoothScrollToPosition(position)
            }
        } catch (e: Exception) {
            // Log and continue - don't crash the app
            android.util.Log.e("ComposeSafeRecyclerView", "Error smooth scrolling to position $position", e)
        }
    }
}