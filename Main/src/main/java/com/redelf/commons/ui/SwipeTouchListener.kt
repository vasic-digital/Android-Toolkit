/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.redelf.commons.ui

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import com.redelf.commons.logging.Console

class SwipeTouchListener(private val swipeView: View) : View.OnTouchListener {

    private val tag = "SWIPE TOUCH LISTENER ::"

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

                    Console.log("$tag translationY: ${swipeView.translationY}")

                    if (swipeView.translationY > 30) {

                        animateSwipeView()
                    }

                    return true
                }

                MotionEvent.ACTION_MOVE -> {

                    if (tracking) {

                        val diff = it.y - startY

                        if (diff > 0) {

                            swipeView.translationY = diff

                            if (!isDragStarted) {

                                isDragStarted = true
                                swipeListener?.onDragStart()
                            }

                        } else {

                            // Ignore the UP direction for now
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

    private fun animateSwipeView() { // TODO: Add animation for/if UP direction

        val parentHeight = swipeView.height
        val tag = "$tag Animate swipe view ::"

        Console.log("$tag Parent height: $parentHeight")

        val currentPosition = swipeView.translationY
        val animateTo = -parentHeight.toFloat()

        Console.log("$tag Animate to: $animateTo")

        val animator = ObjectAnimator.ofFloat(

            swipeView,
            "translationY",
            currentPosition,
            -animateTo

        ).setDuration(500)

        animator.addListener(

                object : Animator.AnimatorListener {

                    override fun onAnimationStart(animation: Animator) {

                        // Ignore
                    }

                    override fun onAnimationEnd(animation: Animator) {

                        swipeListener?.onDismissed()
                    }

                    override fun onAnimationCancel(animation: Animator) {

                        swipeListener?.onDismissed()
                    }

                    override fun onAnimationRepeat(animation: Animator) {

                        // Ignore
                    }
                }
            )

        animator.start()
    }
}