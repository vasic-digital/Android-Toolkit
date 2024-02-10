package com.redelf.commons.ui

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import timber.log.Timber

class SwipeTouchListener(private val swipeView: View) : View.OnTouchListener {

    interface SwipeListener {

        fun onDragStart()

        fun onDragStop()

        fun onDismissed()
    }

    private var tracking = false
    private var startY: Float = 0.0f
    private var isDragStarted = false
    private var swipeListener: SwipeListener? = null

    fun setSwipeListener(swipeListener: SwipeListener?) {

        this.swipeListener = swipeListener

        if (swipeListener == null) {

            swipeView.setOnTouchListener(null)

        } else {

            swipeView.setOnTouchListener(this)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {

        Timber.v("On touch: $event")

        event?.let {

            when (it.action) {

                MotionEvent.ACTION_DOWN -> {

                    val hitRect = Rect()
                    swipeView.getHitRect(hitRect)

                    if (hitRect.contains(event.x.toInt(), event.y.toInt())) {

                        tracking = true
                    }

                    startY = it.y

                    return true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {

                    tracking = false

                    animateSwipeView()

                    return true
                }

                MotionEvent.ACTION_MOVE -> {

                    if (tracking) {

                        swipeView.translationY = it.y - startY

                        if (!isDragStarted) {

                            isDragStarted = true
                            swipeListener?.onDragStart()
                        }
                    }

                    return true
                }

                else -> {

                    false
                }
            }
        }

        return false
    }

    private fun animateSwipeView() { //parentHeight: Int

//        val halfHeight = parentHeight / 2
        val currentPosition = swipeView.translationY
        var animateTo = 0.0f

//        if (currentPosition < -halfHeight) {
//
//            animateTo = (-parentHeight).toFloat()
//
//        } else if (currentPosition > halfHeight) {
//
//            animateTo = parentHeight.toFloat()
//        }

        if (animateTo == 0.0f) {

            swipeListener?.onDragStop()
            isDragStarted = false

        } else {

            swipeListener?.onDismissed()
        }

        ObjectAnimator.ofFloat(swipeView, "translationY", currentPosition, animateTo)
            .setDuration(200)
            .start()
    }
}