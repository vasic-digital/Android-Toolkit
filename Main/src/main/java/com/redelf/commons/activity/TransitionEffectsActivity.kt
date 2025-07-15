package com.redelf.commons.activity

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.redelf.commons.extensions.finishWithTransition
import com.redelf.commons.extensions.getAnimationResource
import com.redelf.commons.extensions.startActivityWithTransition
import com.redelf.commons.logging.Console
import java.lang.annotation.Inherited
import androidx.core.graphics.drawable.toDrawable

abstract class TransitionEffectsActivity : AppCompatActivity() {

    protected open val background = Color.WHITE

    private val tag = "Transition effects :: Who='${this::class.simpleName}' ::"

    companion object {

        private val transitionCache = mutableMapOf<Class<*>, TransitionEffects?>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        Console.log("$tag onCreate")

        window.setBackgroundDrawable(background.toDrawable())

        super.onCreate(savedInstanceState)
    }

    override fun finish() {
        super.finish()

        applyExitTransition("finish")
    }

    override fun onResume() {
        super.onResume()

        applyEnterTransition("onResume")
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun overridePendingTransition(enterAnim: Int, exitAnim: Int, backgroundColor: Int) {

        if (enterAnim == 0 && exitAnim == 0) {

            val tag = "$tag  Do override pending transition (with background) with no transition ::"

            if (hasTransitionAssigned("overridePendingTransition")) {

                Console.log("$tag SKIPPED")

            } else {

                Console.log("$tag APPLIED")

                super.overridePendingTransition(enterAnim, exitAnim, backgroundColor)
            }

        } else {

            Console.log("$tag  Do override pending transition (with background)")

            super.overridePendingTransition(enterAnim, exitAnim, backgroundColor)
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun overridePendingTransition(enterAnim: Int, exitAnim: Int) {

        if (enterAnim == 0 && exitAnim == 0) {

            val tag = "$tag  Do override pending transition with no transition ::"

            if (hasTransitionAssigned("overridePendingTransition")) {

                Console.log("$tag SKIPPED")

            } else {

                Console.log("$tag APPLIED")

                super.overridePendingTransition(enterAnim, exitAnim)
            }

        } else {

            Console.log("$tag  Do override pending transition")

            super.overridePendingTransition(enterAnim, exitAnim)
        }
    }

    @Suppress("DEPRECATION")
    private fun applyEnterTransition(from: String) {

        val tag = "$tag Apply transition :: Enter :: From='$from' ::"

        Console.log("$tag  START")

        val transition = getTransitionAnnotation("applyEnterTransition(from='$from')")

        transition?.let {

            val enter = getAnimationResource(it.enter)

            if (enter > 0) {

                overridePendingTransition(enter, 0, background)

                Console.debug("$tag  Pending transition override")

            } else {

                Console.log("$tag  Pending transition override skipped")
            }
        }

        Console.log("$tag  END")
    }

    @Suppress("DEPRECATION")
    private fun applyExitTransition(from: String) {

        val tag = "$tag Apply transition :: Exit :: From='$from' ::"

        Console.log("$tag START")

        val transition = getTransitionAnnotation("applyExitTransition(from='$from')")

        transition?.let {

            val exit = getAnimationResource(it.exit)

            if (exit > 0) {

                overridePendingTransition(0, exit, background)

                Console.debug("$tag Pending transition override")

            } else {

                Console.log("$tag Pending transition override skipped")
            }
        }

        Console.log("$tag END")
    }

    fun hasTransitionAssigned(from: String): Boolean {

        val tag = "$tag Has annotation :: From='$from' ::"

        getTransitionAnnotation("hasTransitionAssigned(from='$from')")?.let {

            Console.log("$tag END :: Does have")

            return true
        }

        Console.log("$tag END :: Does not have")

        return false
    }

    private fun getTransitionAnnotation(from: String): TransitionEffects? {

        val tag = "$tag Get annotation :: From='$from' ::"

        Console.log("$tag  START")

        val clazz = this::class.java

        val result =  transitionCache.getOrPut(clazz) {

            clazz.getAnnotation(TransitionEffects::class.java)
                ?: if (TransitionEffects::class.java.isAnnotationPresent(Inherited::class.java)) {

                    clazz.superclass?.getAnnotation(TransitionEffects::class.java)

                } else null
        }

        result?.let {

            Console.debug(

                "$tag Get annotation :: END :: " +
                        "Transition(enter='${it.enter},exit=${it.exit}')"
            )

            return it
        }

        Console.log("$tag Get annotation :: END :: No transition")

        return null
    }
}