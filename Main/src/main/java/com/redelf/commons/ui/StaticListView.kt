package com.redelf.commons.ui

import android.view.View
import android.widget.FrameLayout
import android.widget.ListView
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.ConcurrentHashMap

class StaticListView<T>(

    private val container: ListView,
    private var adapter: RecyclerView.Adapter<T>

) where T : RecyclerView.ViewHolder {

    private val views = ConcurrentHashMap<Int, T>()
    private val parent = FrameLayout(container.context)

    init {

        populate()
    }

    fun setAdapter(adapter: RecyclerView.Adapter<T>) {

        this.adapter = adapter

        populate()
    }

    fun notifyDataSetChanged() {

        populate()
    }

    private fun populate() {

        container.post {

            // TODO: [IN_PROGRESS]
        }
    }

    fun getView(position: Int): View {

        val viewType = adapter.getItemViewType(position)
        var viewHolder = views[position]

        if (viewHolder == null) {

            viewHolder = adapter.onCreateViewHolder(parent, viewType)
            views[position] = viewHolder
        }

        adapter.onBindViewHolder(viewHolder, position)

        return viewHolder.itemView
    }
}