package com.redelf.commons.activity

import android.content.Intent
import com.redelf.commons.extensions.finishWithTransition
import com.redelf.commons.extensions.getAnimationResource
import com.redelf.commons.extensions.startActivityWithTransition

abstract class TransitionEffectsActivity : StatefulActivity() {

    override fun startActivity(intent: Intent) {
        super.startActivity(intent)

        applyExitTransition()
    }

    override fun finish() {
        super.finish()

        applyExitTransition()
    }

    override fun onResume() {
        super.onResume()

        applyEnterTransition()
    }

    @Suppress("DEPRECATION")
    private fun applyEnterTransition() {

        val transition = getTransitionAnnotation()

        if (transition != null) {

            val enter = getAnimationResource(transition.enter)

            overridePendingTransition(enter, 0)
        }
    }

    @Suppress("DEPRECATION")
    private fun applyExitTransition() {

        val transition = getTransitionAnnotation()

        if (transition != null) {

            val exit = getAnimationResource(transition.exit)

            overridePendingTransition(0, exit)
        }
    }

    private fun getTransitionAnnotation(): TransitionEffects? {

        val clazz: Class<*> = this::class.java

        while (clazz != Any::class.java) {

            clazz.getAnnotation(TransitionEffects::class.java)?.let {

                return it
            }
        }

        return null
    }
}