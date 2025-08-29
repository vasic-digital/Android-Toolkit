package com.redelf.commons.ui

import android.view.View
import android.widget.FrameLayout
import android.widget.ListView
import androidx.core.view.contains
import androidx.recyclerview.widget.RecyclerView
import com.redelf.commons.data.wrapper.list.ListWrapper
import com.redelf.commons.management.DataManagement
import com.redelf.commons.obtain.Obtain
import java.util.concurrent.ConcurrentHashMap

class ListWrapperView<T, I, M : DataManagement<*>, HOLDER>(

    private val container: ListView,
    private var adapter: RecyclerView.Adapter<HOLDER>,
    private val dataObtain: Obtain<ListWrapper<T, I, M>>

) where HOLDER : RecyclerView.ViewHolder {

    private val views = ConcurrentHashMap<Int, HOLDER>()
    private val parent = FrameLayout(container.context)

    init {

        populate()
    }

    fun setAdapter(adapter: RecyclerView.Adapter<HOLDER>) {

        this.adapter = adapter

        populate()
    }

    fun notifyDataSetChanged() {

        populate()
    }

    private fun populate() {

        container.post {

            val data = dataObtain.obtain()
            val items = data.getList()
            val size = items.size

            for (i in 0 until size) {

                val item = items[i]
                val v = getView(i)
                val identifier = data.getIdentifier(item)

                if (!container.contains(v)) {

                    container.addView(v)
                }
            }
        }
    }

    private fun getView(position: Int): View {

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