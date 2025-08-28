package com.redelf.commons.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.redelf.commons.logging.Console

/**
 * A ListView implementation that dynamically manages child views instead of using RecyclerView.
 * This implementation avoids the jitters and flashing issues associated with RecyclerView
 * by directly managing child views without recycling.
 *
 * @param context The Context the view is running in
 * @param attrs The attributes of the XML tag that is inflating the view
 * @param defStyleAttr An attribute in the current theme that contains a reference to a style
 */
class SafeListView @JvmOverloads constructor(

    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0

) : LinearLayout(context, attrs, defStyleAttr) {

    init {

        orientation = VERTICAL
    }

    /**
     * Interface for providing view holders for items
     */
    interface ViewHolderProvider<T> {

        fun onCreateViewHolder(parent: SafeListView, viewType: Int): ViewHolder<T>

        fun onBindViewHolder(viewHolder: ViewHolder<T>, item: T, position: Int)

        fun getItemViewType(item: T): Int

        fun getItemId(item: T): Long
    }

    /**
     * Abstract class for view holders
     */
    abstract class ViewHolder<T>(val view: View) {

        var item: T? = null
        var position: Int = -1
        var id: Long = -1
        
        open fun bind(item: T, position: Int, id: Long) {

            this.item = item
            this.position = position
            this.id = id
        }
    }

    private var viewHolderProvider: ViewHolderProvider<*>? = null
    private val itemIds = mutableListOf<Long>()
    private var scrollToBottom = false

    /**
     * Set the view holder provider for this list
     */
    fun <T> setViewHolderProvider(provider: ViewHolderProvider<T>) {
        this.viewHolderProvider = provider as ViewHolderProvider<*>
    }

    /**
     * Update the list with new data
     */
    fun <T> updateData(data: List<T>) {
        viewHolderProvider?.let { provider ->
            try {
                updateViews(data, provider as ViewHolderProvider<T>)
            } catch (e: Throwable) {
                Console.error("SafeListView :: Error updating data='${e.message}'")
            }
        }
    }

    /**
     * Update views based on data
     */
    private fun <T> updateViews(data: List<T>, provider: ViewHolderProvider<T>) {
        // If we have no views yet, just add all items
        if (childCount == 0) {
            addAllViews(data, provider)
            return
        }

        // Check if we should recreate the whole list
        val shouldRecreate = shouldRecreateList(data, provider)
        
        if (shouldRecreate) {
            removeAllViews()
            itemIds.clear()
            addAllViews(data, provider)
            return
        }

        // Update existing views and add/remove as needed
        updateExistingViews(data, provider)
    }

    /**
     * Determine if we should recreate the whole list
     */
    private fun <T> shouldRecreateList(data: List<T>, provider: ViewHolderProvider<T>): Boolean {
        // If data size is very different, recreate
        if (Math.abs(childCount - data.size) > data.size / 2) {
            return true
        }

        // Check if item IDs match
        if (data.size != itemIds.size) {
            return true
        }

        // For small lists, check if all IDs are the same
        if (data.size <= 20) {
            for (i in data.indices) {
                val itemId = provider.getItemId(data[i])
                if (i >= itemIds.size || itemIds[i] != itemId) {
                    return true
                }
            }
            return false
        }

        // For larger lists, just check first and last few items
        val checkCount = Math.min(5, data.size)
        for (i in 0 until checkCount) {
            val itemId = provider.getItemId(data[i])
            if (i >= itemIds.size || itemIds[i] != itemId) {
                return true
            }
        }
        
        for (i in (data.size - checkCount) until data.size) {
            val itemId = provider.getItemId(data[i])
            if (i >= itemIds.size || itemIds[i] != itemId) {
                return true
            }
        }

        return false
    }

    /**
     * Add all views for the data
     */
    private fun <T> addAllViews(data: List<T>, provider: ViewHolderProvider<T>) {
        for (i in data.indices) {
            val item = data[i]
            val viewType = provider.getItemViewType(item)
            val viewHolder = provider.onCreateViewHolder(this, viewType)
            val itemId = provider.getItemId(item)
            
            viewHolder.bind(item, i, itemId)
            provider.onBindViewHolder(viewHolder, item, i)
            
            addView(viewHolder.view)
            itemIds.add(itemId)
        }
    }

    /**
     * Update existing views and add/remove as needed
     */
    private fun <T> updateExistingViews(data: List<T>, provider: ViewHolderProvider<T>) {
        val newDataIds = data.map { provider.getItemId(it) }
        
        // Update existing items
        val minSize = Math.min(childCount, data.size)
        for (i in 0 until minSize) {
            val viewHolder = getChildAt(i)?.tag as? ViewHolder<T>
            if (viewHolder != null) {
                val item = data[i]
                viewHolder.bind(item, i, provider.getItemId(item))
                provider.onBindViewHolder(viewHolder, item, i)
            }
        }
        
        // Add new items if data is larger
        if (data.size > childCount) {
            for (i in childCount until data.size) {
                val item = data[i]
                val viewType = provider.getItemViewType(item)
                val viewHolder = provider.onCreateViewHolder(this, viewType)
                val itemId = provider.getItemId(item)
                
                viewHolder.bind(item, i, itemId)
                provider.onBindViewHolder(viewHolder, item, i)
                
                addView(viewHolder.view)
                itemIds.add(itemId)
            }
        }
        // Remove extra views if data is smaller
        else if (data.size < childCount) {
            for (i in childCount - 1 downTo data.size) {
                removeViewAt(i)
                if (itemIds.size > i) {
                    itemIds.removeAt(i)
                }
            }
        }
        
        // Update itemIds list
        itemIds.clear()
        itemIds.addAll(newDataIds)
    }

    /**
     * Add item to the top of the list
     */
    fun <T> addItemToTop(item: T, provider: ViewHolderProvider<T>) {
        val viewType = provider.getItemViewType(item)
        val viewHolder = provider.onCreateViewHolder(this, viewType)
        val itemId = provider.getItemId(item)
        
        viewHolder.bind(item, 0, itemId)
        provider.onBindViewHolder(viewHolder, item, 0)
        
        addView(viewHolder.view, 0)
        itemIds.add(0, itemId)
        
        // Update positions of other items
        for (i in 1 until childCount) {
            val vh = getChildAt(i)?.tag as? ViewHolder<T>
            vh?.position = i
        }
    }

    /**
     * Add item to the bottom of the list
     */
    fun <T> addItemToBottom(item: T, provider: ViewHolderProvider<T>) {
        val viewType = provider.getItemViewType(item)
        val viewHolder = provider.onCreateViewHolder(this, viewType)
        val itemId = provider.getItemId(item)
        val position = childCount
        
        viewHolder.bind(item, position, itemId)
        provider.onBindViewHolder(viewHolder, item, position)
        
        addView(viewHolder.view)
        itemIds.add(itemId)
    }

    /**
     * Scroll to the bottom of the list
     */
    @SuppressLint("WrongConstant")
    fun scrollToBottom() {
        scrollToBottom = true
        post {
            if (scrollToBottom && height > 0) {
                scrollTo(0, height)
                scrollToBottom = false
            }
        }
    }

    /**
     * Scroll to the top of the list
     */
    fun scrollToTop() {
        post {
            scrollTo(0, 0)
        }
    }
}