package com.redelf.commons.ui

import android.widget.ListView
import androidx.recyclerview.widget.RecyclerView

class StaticListView(

    private val container: ListView

) {

    private var adapter: RecyclerView.Adapter<*>? = null

    fun setAdapter(adapter: RecyclerView.Adapter<*>) {

        this.adapter = adapter

        populate()
    }

    fun removeAdapter() {

        this.adapter = null

        populate()
    }

    fun notifyDataSetChanged() {

        populate()
    }

    private fun populate() {

        container.post {

            adapter?.let {

                // TODO: [IN_PROGRESS]
            }

            if (adapter == null) {

                container.removeAllViews()
            }
        }
    }
}