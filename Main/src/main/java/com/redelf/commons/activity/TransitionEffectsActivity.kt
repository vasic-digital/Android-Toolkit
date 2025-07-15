package com.redelf.commons.activity

import android.content.Intent
import com.redelf.commons.extensions.finishWithTransition
import com.redelf.commons.extensions.getAnimationResource
import com.redelf.commons.extensions.startActivityWithTransition
import java.lang.annotation.Inherited

abstract class TransitionEffectsActivity : StatefulActivity() {

    companion object {

        private val transitionCache = mutableMapOf<Class<*>, TransitionEffects?>()
    }

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

        transition?.let {

            val enter = getAnimationResource(it.enter)

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

        val clazz = this::class.java

        return transitionCache.getOrPut(clazz) {

            clazz.getAnnotation(TransitionEffects::class.java)
                ?: if (TransitionEffects::class.java.isAnnotationPresent(Inherited::class.java)) {

                    clazz.superclass?.getAnnotation(TransitionEffects::class.java)

                } else null
        }
    }
}