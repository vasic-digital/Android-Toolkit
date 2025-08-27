package com.redelf.commons.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import com.redelf.commons.logging.Console
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CopyOnWriteArrayList

class WrappedSafeView<T> @JvmOverloads constructor(

    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0

) : FrameLayout(context, attrs, defStyleAttr) where T : RecyclerView.ViewHolder {

    // Extension for safe bindViewHolder call
    private fun RecyclerView.Adapter<T>.bindViewHolderSafe(

        holder: RecyclerView.ViewHolder,
        position: Int

    ) {

        try {

            // This uses reflection-like approach but is actually type-safe due to how RecyclerView works
            @Suppress("UNCHECKED_CAST")
            (this as RecyclerView.Adapter<RecyclerView.ViewHolder>).bindViewHolder(holder, position)

        } catch (e: Throwable) {

            Console.error(e)
        }
    }

    private val composeView = ComposeView(context)
    private var adapter: RecyclerView.Adapter<T>? = null
    private val dataSetMutex = Mutex()
    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val currentDataSet = CopyOnWriteArrayList<AdapterItem>()
    private val viewHolderCache = mutableMapOf<Int, RecyclerView.ViewHolder>()

    init {

        addView(composeView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        composeView.setContent {

            WrappedSafeContent()
        }
    }

    fun setAdapter(adapter: RecyclerView.Adapter<T>) {

        synchronized(this) {

            this.adapter?.unregisterAdapterDataObserver(adapterDataObserver)
            this.adapter = adapter

            adapter.registerAdapterDataObserver(adapterDataObserver)

            viewHolderCache.clear()
            updateDataSetSafe()
        }
    }

    fun getAdapter(): RecyclerView.Adapter<T>? = synchronized(this) { adapter }

    fun swapAdapter(newAdapter: RecyclerView.Adapter<T>?, removeAndRecycleExistingViews: Boolean) {

        synchronized(this) {

            adapter?.unregisterAdapterDataObserver(adapterDataObserver)
            adapter = newAdapter
            newAdapter?.registerAdapterDataObserver(adapterDataObserver)
            viewHolderCache.clear()
            updateDataSetSafe()
        }
    }

    fun setItemAnimator(animator: RecyclerView.ItemAnimator?) {
        // Compose handles animations internally
    }

    fun setLayoutManager(layoutManager: RecyclerView.LayoutManager?) {
        // Compose handles layout internally with LazyColumn
    }

    fun setHasFixedSize(hasFixedSize: Boolean) {
        // Compose handles sizing internally
    }

    fun addItemDecoration(decor: RecyclerView.ItemDecoration) {
        // Compose handles decorations through modifiers
    }

    fun removeItemDecoration(decor: RecyclerView.ItemDecoration) {
        // Compose handles decorations internally
    }

    fun smoothScrollToPosition(position: Int) {
        // Could be implemented with state management
    }

    fun scrollToPosition(position: Int) {
        // Could be implemented with state management
    }

    private fun updateDataSetSafe() {

        coroutineScope.launch {

            dataSetMutex.withLock {

                currentDataSet.clear()

                val itemCount = adapter?.safeGetItemCount() ?: 0

                for (i in 0 until itemCount) {

                    val viewType = adapter?.safeGetItemViewType(i) ?: 0
                    val itemId = adapter?.getItemId(i) ?: RecyclerView.NO_ID
                    currentDataSet.add(AdapterItem(itemId, i, viewType))
                }
            }
        }
    }

    @Composable
    private fun WrappedSafeContent() {

        val items = remember { currentDataSet.toList() }

        if (items.isNotEmpty()) {

            val listState = rememberLazyListState()

            LazyColumn(

                state = listState,
                modifier = Modifier.fillMaxSize()

            ) {

                items(items, key = { it.id }) { item ->

                    Box(modifier = Modifier.fillMaxSize()) {

                        ComposeItemView(item = item)
                    }
                }
            }

        } else {

            Box(modifier = Modifier.fillMaxSize()) {

                // Empty state or loading indicator can be added here
            }
        }
    }

    @Composable
    private fun ComposeItemView(item: AdapterItem) {

        val currentAdapter = rememberUpdatedState(adapter)

        AndroidView(

            factory = { context ->

                // Create a dummy RecyclerView for the adapter to use
                val dummyRecyclerView = RecyclerView(context).apply {

                    layoutParams = ViewGroup.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                    )

                    // Make it invisible since we only need it for view holder creation
                    visibility = GONE
                }

                // Get or create view holder
                val viewHolder = viewHolderCache.getOrPut(item.position) {

                    currentAdapter.value?.createViewHolder(dummyRecyclerView, item.viewType)
                        ?: createFallbackViewHolder(context)
                }

                // Bind data to the view holder
                currentAdapter.value?.bindViewHolderSafe(viewHolder, item.position)

                viewHolder.itemView.apply {

                    layoutParams = ViewGroup.LayoutParams(

                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                    )
                }
            },

            update = { view ->

                // Find the view holder for this position and update if needed
                viewHolderCache[item.position]?.let { viewHolder ->

                    currentAdapter.value?.bindViewHolderSafe(viewHolder, item.position)
                }
            }
        )
    }

    private fun createFallbackViewHolder(context: Context): RecyclerView.ViewHolder {

        val fallbackView = View(context).apply {

            layoutParams = ViewGroup.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        }

        return ConcreteViewHolder(fallbackView)
    }

    // Concrete implementation of RecyclerView.ViewHolder
    private class ConcreteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private data class AdapterItem(val id: Long, val position: Int, val viewType: Int)

    private val adapterDataObserver = object : RecyclerView.AdapterDataObserver() {

        override fun onChanged() {

            updateDataSetSafe()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {

            updateDataSetSafe()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {

            updateDataSetSafe()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {

            // Remove cached view holders for removed positions
            synchronized(viewHolderCache) {

                val positionsToRemove =
                    viewHolderCache.keys.filter { it >= positionStart && it < positionStart + itemCount }

                positionsToRemove.forEach { viewHolderCache.remove(it) }
            }

            updateDataSetSafe()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {

            updateDataSetSafe()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        synchronized(this) {

            adapter?.unregisterAdapterDataObserver(adapterDataObserver)
        }

        viewHolderCache.clear()

        coroutineScope.coroutineContext.cancel()
    }
}

// Extension for safe item count access
private fun <T> RecyclerView.Adapter<T>.safeGetItemCount(): Int where T : RecyclerView.ViewHolder {

    return try {

        itemCount

    } catch (e: Throwable) {

        Console.error(e)

        0
    }
}

// Extension for safe view type access
private fun <T> RecyclerView.Adapter<T>.safeGetItemViewType(position: Int): Int where T : RecyclerView.ViewHolder {

    return try {

        getItemViewType(position)

    } catch (e: Throwable) {

        Console.error(e)

        0
    }
}
