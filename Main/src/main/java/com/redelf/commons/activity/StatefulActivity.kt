package com.redelf.commons.activity

import androidx.appcompat.app.AppCompatActivity
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import java.util.concurrent.atomic.AtomicBoolean

abstract class StatefulActivity : AppCompatActivity(), ActivityActiveStateSubscription {

    private val paused = AtomicBoolean()
    private val activeState = AtomicBoolean()
    private val activeStateCallbacks = Callbacks<ActivityActiveStateListener>("resumeCallbacks")

    override fun register(subscriber: ActivityActiveStateListener) {

        if (activeStateCallbacks.isRegistered(subscriber)) {

            return
        }

        activeStateCallbacks.register(subscriber)
    }

    override fun unregister(subscriber: ActivityActiveStateListener) {

        activeStateCallbacks.unregister(subscriber)
    }

    fun isPaused() = paused.get()

    fun isActive() = activeState.get()

    private fun onActiveStateChanged(active: Boolean) {

        activeState.set(active)

        activeStateCallbacks.doOnAll(

            object : CallbackOperation<ActivityActiveStateListener> {

                override fun perform(callback: ActivityActiveStateListener) {

                    callback.onActivityStateChanged(this@StatefulActivity, active)
                }
            },

            "onActivityStateChanged"
        )
    }

    override fun isRegistered(subscriber: ActivityActiveStateListener): Boolean {

        return activeStateCallbacks.isRegistered(subscriber)
    }

    override fun onPause() {

        paused.set(true)

        super.onPause()

        onActiveStateChanged(false)
    }

    override fun onResume() {

        paused.set(false)

        super.onResume()

        onActiveStateChanged(true)
    }

    override fun onDestroy() {

        activeStateCallbacks.clear()

        super.onDestroy()
    }
}