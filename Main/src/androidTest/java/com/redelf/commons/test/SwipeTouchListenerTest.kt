package com.redelf.commons.test

import android.view.MotionEvent
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.redelf.commons.ui.SwipeTouchListener

@RunWith(AndroidJUnit4::class)
class SwipeTouchListenerTest {

    @Test
    fun testSwipeTouchListenerInitialization() {
        val view = View(null)
        val listener = SwipeTouchListener(view)
        assertTrue(listener != null)
    }

    @Test
    fun testOnTouchActionDown() {
        val view = View(null)
        val listener = SwipeTouchListener(view)
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0)
        val result = listener.onTouch(view, event)
        assertTrue(result)
    }
}