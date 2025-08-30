package com.redelf.commons.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import com.redelf.commons.logging.Console

class SafeRecyclerView @JvmOverloads constructor(

    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0

) : RecyclerView(context, attrs, defStyleAttr) {

    @SuppressLint("NotifyDataSetChanged")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

        try {

            super.onLayout(changed, l, t, r, b)

        } catch (e: Throwable) {

            Console.error("SafeRecyclerView :: Error='${e.message}'")
        }
    }

    override fun onDraw(c: Canvas) {

        try {

            super.onDraw(c)

        } catch (e: Throwable) {

            Console.error("SafeRecyclerView :: Error='${e.message}'")
        }
    }
}
