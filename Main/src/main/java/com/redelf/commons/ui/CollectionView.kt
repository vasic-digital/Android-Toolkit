package com.redelf.commons.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.redelf.commons.logging.Console
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * A crash-resistant RecyclerView that handles all common failure scenarios
 * 
 * Features:
 *
 * - No crashes when invalid state occurs
 * - Thread-safe adapter operations
 * - Automatic error recovery
 * - Safe scrolling operations
 * - IndexOutOfBounds protection
 * - Inconsistent state handling
 * - Memory leak prevention
 *
 */
class CollectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    // Initialize atomic fields FIRST to avoid null pointer exceptions during construction
    private val isDestroyed = AtomicBoolean(false)
    private val isLayoutFrozen = AtomicBoolean(false)
    private val isProcessingUpdates = AtomicBoolean(false)
    private val pendingNotifyDataSetChanged = AtomicBoolean(false)
    private var lastKnownItemCount = AtomicInteger(0)
    private val isObserverRegistered = AtomicBoolean(false)
    
    // Initialize handler and queue after atomic fields
    private val mainHandler = Handler(Looper.getMainLooper())
    private val updateQueue = java.util.concurrent.LinkedBlockingQueue<() -> Unit>()
    
    // Safe data observer to prevent crashes during adapter changes
    private val safeDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            if (!isDestroyed.get()) {
                // Queue the update instead of immediate execution to prevent flashes
                queueUpdate {
                    if (!isDestroyed.get() && adapter != null) {
                        try {
                            val currentItemCount = adapter?.itemCount ?: 0
                            lastKnownItemCount.set(currentItemCount)
                            super.onChanged()
                        } catch (e: Exception) {
                            Console.error("SafeRecyclerView :: onChanged error: ${e.message}")
                        }
                    }
                }
            }
        }
        
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            if (!isDestroyed.get() && isValidRange(positionStart, itemCount)) {
                queueUpdate {
                    if (!isDestroyed.get() && isValidRange(positionStart, itemCount)) {
                        try {
                            super.onItemRangeChanged(positionStart, itemCount)
                        } catch (e: Exception) {
                            Console.error("SafeRecyclerView :: onItemRangeChanged error: ${e.message}")
                            // Fallback to full refresh if range update fails
                            fallbackToFullUpdate()
                        }
                    }
                }
            }
        }
        
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            if (!isDestroyed.get() && isValidRange(positionStart, itemCount)) {
                queueUpdate {
                    if (!isDestroyed.get() && isValidRange(positionStart, itemCount)) {
                        try {
                            super.onItemRangeChanged(positionStart, itemCount, payload)
                        } catch (e: Exception) {
                            Console.error("SafeRecyclerView :: onItemRangeChanged with payload error: ${e.message}")
                            fallbackToFullUpdate()
                        }
                    }
                }
            }
        }
        
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            if (!isDestroyed.get() && positionStart >= 0 && itemCount > 0) {
                queueUpdate {
                    if (!isDestroyed.get()) {
                        try {
                            val currentItemCount = adapter?.itemCount ?: 0
                            if (positionStart <= currentItemCount) {
                                super.onItemRangeInserted(positionStart, itemCount)
                                lastKnownItemCount.set(currentItemCount)
                            }
                        } catch (e: Exception) {
                            Console.error("SafeRecyclerView :: onItemRangeInserted error: ${e.message}")
                            fallbackToFullUpdate()
                        }
                    }
                }
            }
        }
        
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            if (!isDestroyed.get() && positionStart >= 0 && itemCount > 0) {
                queueUpdate {
                    if (!isDestroyed.get()) {
                        try {
                            val currentItemCount = adapter?.itemCount ?: 0
                            super.onItemRangeRemoved(positionStart, itemCount)
                            lastKnownItemCount.set(currentItemCount)
                        } catch (e: Exception) {
                            Console.error("SafeRecyclerView :: onItemRangeRemoved error: ${e.message}")
                            fallbackToFullUpdate()
                        }
                    }
                }
            }
        }
        
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            if (!isDestroyed.get() && isValidRange(fromPosition, itemCount) && isValidRange(toPosition, itemCount)) {
                queueUpdate {
                    if (!isDestroyed.get() && isValidRange(fromPosition, itemCount) && isValidRange(toPosition, itemCount)) {
                        try {
                            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                        } catch (e: Exception) {
                            Console.error("SafeRecyclerView :: onItemRangeMoved error: ${e.message}")
                            fallbackToFullUpdate()
                        }
                    }
                }
            }
        }
        
        private fun isValidRange(position: Int, count: Int): Boolean {
            val adapter = this@CollectionView.adapter ?: return false
            val itemCount = try { adapter.itemCount } catch (e: Exception) { return false }
            return position >= 0 && count > 0 && position < itemCount && (position + count) <= itemCount
        }
        
        private fun fallbackToFullUpdate() {
            if (!pendingNotifyDataSetChanged.getAndSet(true)) {
                mainHandler.postDelayed({
                    try {
                        if (!isDestroyed.get()) {
                            super.onChanged()
                        }
                    } finally {
                        pendingNotifyDataSetChanged.set(false)
                    }
                }, 16) // One frame delay to prevent rapid updates
            }
        }
    }

    init {
        // Set up safe defaults
        setHasFixedSize(true)
        setItemViewCacheSize(20)
        // Note: isDrawingCacheEnabled and isAnimationCacheEnabled are deprecated in newer Android versions
        // but we'll keep them for backward compatibility with try-catch
        try {
            @Suppress("DEPRECATION")
            isDrawingCacheEnabled = false
            @Suppress("DEPRECATION") 
            isAnimationCacheEnabled = false
        } catch (e: Exception) {
            // Ignore if deprecated methods are not available
        }
    }

    /**
     * Safe post operation that checks if view is still valid
     */
    private fun safePost(action: () -> Unit) {
        if (!isDestroyed.get()) {
            mainHandler.post {
                if (!isDestroyed.get()) {
                    try {
                        action()
                    } catch (e: Exception) {
                        Console.error("SafeRecyclerView :: Safe post error: ${e.message}")
                    }
                }
            }
        }
    }
    
    /**
     * Queues updates to prevent flashes and ensure thread safety
     */
    private fun queueUpdate(update: () -> Unit) {
        if (!isDestroyed.get()) {
            updateQueue.offer(update)
            processUpdateQueue()
        }
    }
    
    /**
     * Processes queued updates in a thread-safe manner to prevent visual glitches
     */
    private fun processUpdateQueue() {
        if (!isProcessingUpdates.compareAndSet(false, true)) {
            return // Already processing
        }
        
        mainHandler.post {
            try {
                if (!isDestroyed.get()) {
                    // Process all queued updates in one frame to prevent flickering
                    val updates = mutableListOf<() -> Unit>()
                    var update = updateQueue.poll()
                    while (update != null && updates.size < 100) { // Increased batch size for better performance
                        updates.add(update)
                        update = updateQueue.poll()
                    }
                    
                    // Execute all updates
                    updates.forEach { updateAction ->
                        try {
                            updateAction()
                        } catch (e: Exception) {
                            Console.error("SafeRecyclerView :: Update execution error: ${e.message}")
                        }
                    }
                }
            } finally {
                isProcessingUpdates.set(false)
                // Check if there are more updates to process
                if (!updateQueue.isEmpty() && !isDestroyed.get()) {
                    mainHandler.postDelayed({ 
                        if (!isDestroyed.get()) processUpdateQueue() 
                    }, 16) // Next frame
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (isDestroyed.get() || isLayoutFrozen.get()) return
        
        try {
            super.onLayout(changed, l, t, r, b)
        } catch (e: IndexOutOfBoundsException) {
            Console.error("SafeRecyclerView :: IndexOutOfBounds in onLayout: ${e.message}")
            // Attempt recovery by clearing adapter temporarily
            recoverFromInconsistentState()
        } catch (e: IllegalStateException) {
            Console.error("SafeRecyclerView :: IllegalState in onLayout: ${e.message}")
            // Handle inconsistent state
            if (e.message?.contains("inconsistent state") == true) {
                recoverFromInconsistentState()
            }
        } catch (e: Throwable) {
            Console.error("SafeRecyclerView :: Unexpected error in onLayout: ${e.message}")
        }
    }

    override fun onDraw(c: Canvas) {
        if (isDestroyed.get()) return
        
        try {
            super.onDraw(c)
        } catch (e: IndexOutOfBoundsException) {
            Console.error("SafeRecyclerView :: IndexOutOfBounds in onDraw: ${e.message}")
        } catch (e: IllegalStateException) {
            Console.error("SafeRecyclerView :: IllegalState in onDraw: ${e.message}")
        } catch (e: Throwable) {
            Console.error("SafeRecyclerView :: Unexpected error in onDraw: ${e.message}")
        }
    }

    override fun onTouchEvent(e: MotionEvent?): Boolean {
        if (isDestroyed.get() || e == null) return false
        
        return try {
            super.onTouchEvent(e)
        } catch (ex: Exception) {
            Console.error("SafeRecyclerView :: Touch event error: ${ex.message}")
            false
        }
    }

    override fun onInterceptTouchEvent(e: MotionEvent?): Boolean {
        if (isDestroyed.get() || e == null) return false
        
        return try {
            super.onInterceptTouchEvent(e)
        } catch (ex: Exception) {
            Console.error("SafeRecyclerView :: Intercept touch event error: ${ex.message}")
            false
        }
    }

    override fun requestLayout() {
        try {
            // Safe check for initialization state
            val destroyed = try { isDestroyed.get() } catch (e: Exception) { false }
            val layoutFrozen = try { isLayoutFrozen.get() } catch (e: Exception) { false }
            
            if (destroyed || layoutFrozen) return
            
            super.requestLayout()
        } catch (e: Exception) {
            Console.error("SafeRecyclerView :: Request layout error: ${e.message}")
        }
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        try {
            // Safe check for initialization state
            val destroyed = try { isDestroyed.get() } catch (e: Exception) { false }
            
            if (destroyed) return
        
            // Unregister old adapter observer
            if (isObserverRegistered.getAndSet(false)) {
                this.adapter?.unregisterAdapterDataObserver(safeDataObserver)
            }
            
            super.setAdapter(adapter)
            
            // Register new adapter observer
            if (adapter != null && isObserverRegistered.compareAndSet(false, true)) {
                adapter.registerAdapterDataObserver(safeDataObserver)
            }
            
        } catch (e: Exception) {
            Console.error("SafeRecyclerView :: Set adapter error: ${e.message}")
        }
    }

    override fun setLayoutManager(layout: LayoutManager?) {
        try {
            // Safe check for initialization state
            val destroyed = try { isDestroyed.get() } catch (e: Exception) { false }
            
            if (destroyed) return
        
            super.setLayoutManager(layout)
        } catch (e: Exception) {
            Console.error("SafeRecyclerView :: Set layout manager error: ${e.message}")
            // Fallback to LinearLayoutManager
            try {
                super.setLayoutManager(LinearLayoutManager(context))
            } catch (fallbackEx: Exception) {
                Console.error("SafeRecyclerView :: Fallback layout manager failed: ${fallbackEx.message}")
            }
        }
    }

    override fun smoothScrollToPosition(position: Int) {
        if (isDestroyed.get() || position < 0) return
        
        try {
            val adapter = this.adapter
            if (adapter != null && position < adapter.itemCount) {
                super.smoothScrollToPosition(position)
            }
        } catch (e: Exception) {
            Console.error("SafeRecyclerView :: Smooth scroll error: ${e.message}")
        }
    }

    override fun scrollToPosition(position: Int) {
        if (isDestroyed.get() || position < 0) return
        
        try {
            val adapter = this.adapter
            if (adapter != null && position < adapter.itemCount) {
                super.scrollToPosition(position)
            }
        } catch (e: Exception) {
            Console.error("SafeRecyclerView :: Scroll to position error: ${e.message}")
        }
    }

    fun safeNotifyDataSetChanged() {
        if (isDestroyed.get()) return
        
        try {
            adapter?.notifyDataSetChanged()
        } catch (e: Exception) {
            Console.error("SafeRecyclerView :: Notify data set changed error: ${e.message}")
        }
    }

    override fun stopScroll() {
        if (isDestroyed.get()) return
        
        try {
            super.stopScroll()
        } catch (e: Exception) {
            Console.error("SafeRecyclerView :: Stop scroll error: ${e.message}")
        }
    }

    /**
     * Safe method to add item decorations
     */
    override fun addItemDecoration(decor: ItemDecoration) {
        if (isDestroyed.get()) return
        
        try {
            super.addItemDecoration(decor)
        } catch (e: Exception) {
            Console.error("SafeRecyclerView :: Add item decoration error: ${e.message}")
        }
    }

    /**
     * Safe method to remove item decorations
     */
    override fun removeItemDecoration(decor: ItemDecoration) {
        if (isDestroyed.get()) return
        
        try {
            super.removeItemDecoration(decor)
        } catch (e: Exception) {
            Console.error("SafeRecyclerView :: Remove item decoration error: ${e.message}")
        }
    }

    /**
     * Attempts to recover from inconsistent state without visual flashes
     */
    private fun recoverFromInconsistentState() {
        try {
            val currentAdapter = this.adapter
            if (currentAdapter != null) {
                Console.error("SafeRecyclerView :: Attempting gentle recovery from inconsistent state")
                
                // Try gentle recovery first - just request layout
                queueUpdate {
                    try {
                        requestLayout()
                        invalidate()
                    } catch (e: Exception) {
                        Console.error("SafeRecyclerView :: Gentle recovery failed: ${e.message}")
                        // If gentle recovery fails, do more aggressive recovery
                        performAggressiveRecovery(currentAdapter)
                    }
                }
            }
        } catch (e: Exception) {
            Console.error("SafeRecyclerView :: Recovery attempt failed: ${e.message}")
            isLayoutFrozen.set(false)
        }
    }
    
    /**
     * More aggressive recovery when gentle recovery fails
     */
    private fun performAggressiveRecovery(currentAdapter: Adapter<*>) {
        try {
            Console.error("SafeRecyclerView :: Performing aggressive recovery")
            
            // Temporarily freeze layout to prevent flashes
            isLayoutFrozen.set(true)
            
            // Save current scroll position
            val layoutManager = this.layoutManager
            val scrollPosition = when (layoutManager) {
                is LinearLayoutManager -> layoutManager.findFirstVisibleItemPosition()
                else -> 0
            }
            
            // Use a very short delay to minimize visual disruption
            mainHandler.postDelayed({
                try {
                    // Clear and restore adapter
                    super.setAdapter(null)
                    super.setAdapter(currentAdapter)
                    
                    // Restore scroll position if valid
                    val itemCount = try { currentAdapter.itemCount } catch (e: Exception) { 0 }
                    if (scrollPosition >= 0 && scrollPosition < itemCount) {
                        try {
                            scrollToPosition(scrollPosition)
                        } catch (e: Exception) {
                            // Ignore scroll position restoration errors
                        }
                    }
                    
                    isLayoutFrozen.set(false)
                    Console.error("SafeRecyclerView :: Aggressive recovery completed")
                } catch (e: Exception) {
                    Console.error("SafeRecyclerView :: Aggressive recovery failed: ${e.message}")
                    isLayoutFrozen.set(false)
                }
            }, 16) // One frame delay
        } catch (e: Exception) {
            Console.error("SafeRecyclerView :: Aggressive recovery setup failed: ${e.message}")
            isLayoutFrozen.set(false)
        }
    }

    /**
     * Safe method to check if position is valid
     */
    fun isValidPosition(position: Int): Boolean {
        val adapter = this.adapter
        return !isDestroyed.get() && adapter != null && position >= 0 && position < adapter.itemCount
    }

    /**
     * Safe method to get item count
     */
    fun getSafeItemCount(): Int {
        return try {
            adapter?.itemCount ?: 0
        } catch (e: Exception) {
            Console.error("SafeRecyclerView :: Get item count error: ${e.message}")
            0
        }
    }

    /**
     * Safe method to refresh data
     */
    fun safeRefresh() {
        if (isDestroyed.get()) return
        
        safePost {
            try {
                safeNotifyDataSetChanged()
            } catch (e: Exception) {
                Console.error("SafeRecyclerView :: Safe refresh error: ${e.message}")
            }
        }
    }

    /**
     * Force a safe layout pass
     */
    fun safeForcedLayout() {
        if (isDestroyed.get()) return
        
        safePost {
            try {
                requestLayout()
            } catch (e: Exception) {
                Console.error("SafeRecyclerView :: Safe forced layout error: ${e.message}")
            }
        }
    }

    override fun onDetachedFromWindow() {
        try {
            isDestroyed.set(true)
            
            // Clear all pending updates to prevent memory leaks
            updateQueue.clear()
            isProcessingUpdates.set(false)
            
            // Clear pending notifications
            pendingNotifyDataSetChanged.set(false)
            
            // Unregister adapter observer safely
            if (isObserverRegistered.getAndSet(false)) {
                adapter?.unregisterAdapterDataObserver(safeDataObserver)
            }
            
            // Clear animations and layout state
            clearAnimation()
            isLayoutFrozen.set(false)
            
            // Remove any pending callbacks
            mainHandler.removeCallbacksAndMessages(null)
            
            super.onDetachedFromWindow()
        } catch (e: Exception) {
            Console.error("SafeRecyclerView :: Detach from window error: ${e.message}")
        }
    }

    override fun onAttachedToWindow() {
        if (isDestroyed.get()) {
            isDestroyed.set(false) // Reset destroyed state when reattaching
        }
        
        try {
            super.onAttachedToWindow()
            
            // Re-register adapter observer if adapter exists and not already registered
            val currentAdapter = adapter
            if (currentAdapter != null && isObserverRegistered.compareAndSet(false, true)) {
                currentAdapter.registerAdapterDataObserver(safeDataObserver)
            }
            
            // Update last known item count
            lastKnownItemCount.set(currentAdapter?.itemCount ?: 0)
            
        } catch (e: Exception) {
            Console.error("SafeRecyclerView :: Attach to window error: ${e.message}")
        }
    }
    
    /**
     * Clean shutdown method to be called when the view is no longer needed
     */
    fun cleanup() {
        try {
            isDestroyed.set(true)
            updateQueue.clear()
            isProcessingUpdates.set(false)
            pendingNotifyDataSetChanged.set(false)
            isLayoutFrozen.set(false)
            mainHandler.removeCallbacksAndMessages(null)
            if (isObserverRegistered.getAndSet(false)) {
                adapter?.unregisterAdapterDataObserver(safeDataObserver)
            }
        } catch (e: Exception) {
            Console.error("SafeRecyclerView :: Cleanup error: ${e.message}")
        }
    }
}
