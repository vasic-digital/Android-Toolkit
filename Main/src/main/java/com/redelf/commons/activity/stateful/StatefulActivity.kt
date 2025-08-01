package com.redelf.commons.activity.stateful

import android.os.Bundle
import com.redelf.commons.activity.transition.TransitionEffectsActivity
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.extensions.fitInsideSystemBoundaries
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.logging.Console
import java.util.concurrent.atomic.AtomicBoolean

abstract class StatefulActivity : TransitionEffectsActivity(), ActivityActiveStateSubscription {

    protected open val fitInsideSystemBoundaries = true
    protected open val removeFromHistoryOnFinish = false

    private var tagContext = ""
    private val paused = AtomicBoolean()
    private val activeState = AtomicBoolean()
    private val tag = "Stateful :: Activity ::"
    private val activeStateCallbacks = Callbacks<ActivityActiveStateListener>("resumeCallbacks")

    override fun register(subscriber: ActivityActiveStateListener) {

        if (activeStateCallbacks.isRegistered(subscriber)) {

            Console.log(

                "${getLogTag()} ALREADY REGISTERED :: Subscriber = ${subscriber.hashCode()}"
            )

            return
        }

        Console.log(

            "${getLogTag()} REGISTER :: Subscriber = ${subscriber.hashCode()}"
        )

        activeStateCallbacks.register(subscriber)
    }

    override fun unregister(subscriber: ActivityActiveStateListener) {

        Console.log(

            "${getLogTag()} UNREGISTER :: Subscriber = ${subscriber.hashCode()}"
        )

        activeStateCallbacks.unregister(subscriber)
    }

    fun isPaused() = paused.get()

    fun isActive() = activeState.get()

    fun clearTagContext() {

        Console.log("${getLogTag()} TAG CONTEXT :: CLEAR")

        tagContext = ""
    }

    fun setTagContext(context: String) {

        tagContext = context

        Console.log("${getLogTag()} TAG CONTEXT :: SET :: Value = $context")
    }

    private fun onActiveStateChanged(active: Boolean) {

        activeState.set(active)

        Console.log(

            "${getLogTag()} NOTIFY :: Active = $active, " +
                    "Subscribers = ${activeStateCallbacks.getSubscribersCount()}"
        )

        activeStateCallbacks.doOnAll(

            object : CallbackOperation<ActivityActiveStateListener> {

                override fun perform(callback: ActivityActiveStateListener) {

                    Console.log(

                        "${getLogTag()} NOTIFY :: Active = $active, " +
                                "Subscriber = ${callback.hashCode()}"
                    )

                    callback.onActivityStateChanged(this@StatefulActivity, active)
                }
            },

            "onActivityStateChanged"
        )
    }

    override fun isRegistered(subscriber: ActivityActiveStateListener): Boolean {

        val registered = activeStateCallbacks.isRegistered(subscriber)

        if (registered) {

            Console.log("${getLogTag()} IT IS REGISTERED :: ${subscriber.hashCode()}")

        } else {

            Console.log("${getLogTag()} IT IS NOT REGISTERED :: ${subscriber.hashCode()}")
        }

        return registered
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (fitInsideSystemBoundaries) {

            fitInsideSystemBoundaries()
        }
    }

    override fun onPause() {

        paused.set(true)

        super.onPause()

        Console.log("${getLogTag()} ON PAUSE")

        onActiveStateChanged(false)
    }

    override fun onResume() {

        paused.set(false)

        super.onResume()

        Console.log("${getLogTag()} ON RESUME")

        onActiveStateChanged(true)
    }

    override fun onDestroy() {

        val tag = "${getLogTag()} onDestroy :: ${this@StatefulActivity::class.simpleName} ::"

        Console.log("$tag START")

        activeStateCallbacks.doOnAll(

            object : CallbackOperation<ActivityActiveStateListener> {

                override fun perform(callback: ActivityActiveStateListener) {

                    Console.log("$tag NOTIFY :: Destruction :: Subscriber = ${callback.hashCode()}")

                    callback.onDestruction(this@StatefulActivity)

                    activeStateCallbacks.unregister(callback)
                }
            },

            "onDestruction"
        )

        super.onDestroy()

        Console.log("$tag END")
    }

    private fun getLogTag(): String {

        if (isEmpty(tagContext)) {

            return tag
        }

        return "$tagContext :: $tag"
    }

    protected open fun getFinishTag() = "Finish :: Activity = '${this.javaClass.simpleName}' ::"

    @Suppress("DEPRECATION")
    override fun finish() {

        val tag = getFinishTag()

        Console.log("$tag START")

        val hold = com.redelf.commons.R.anim.hold
        overridePendingTransition(hold, hold)

        if (removeFromHistoryOnFinish) {

            finishAndRemoveTask()

        } else {

            super.finish()
        }

        if (!hasTransitionAssigned("finish")) {

            val hold = com.redelf.commons.R.anim.hold
            overridePendingTransition(hold, hold)
        }

        Console.log("$tag END")
    }

    protected fun removeActivityFromHistory() {

        if (removeFromHistoryOnFinish) {

            finishAndRemoveTask()
        }
    }
}