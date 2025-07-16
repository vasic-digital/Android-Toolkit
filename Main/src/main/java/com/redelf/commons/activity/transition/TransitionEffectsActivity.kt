package com.redelf.commons.activity.transition

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.redelf.commons.R
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.getAnimationResource
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.messaging.broadcast.Broadcast
import java.lang.annotation.Inherited


abstract class TransitionEffectsActivity : AppCompatActivity() {

    protected open val background = Color.WHITE
    protected open val backgroundActivity = BackgroundActivity::class.java

    private val tag = "Transition effects :: Who='${this::class.simpleName}' ::"

    companion object {

        private val GROUPS = HashMap<String, HashSet<String>>()

        private val transitionCache = mutableMapOf<Class<*>, TransitionEffects?>()
    }

    protected fun startActivity(intent: Intent, callback: () -> Unit) {

        doStartActivity(intent, callback)
    }

    /*
    *   TODO: Implement groups support for the nested groups and multiple instances of the activity
    */
    override fun startActivity(intent: Intent) {

        val group = getGroup(intent)
        val transition = getTransitionAnnotation("startActivity")

        if (group.isEmpty()) {

            doStartActivity(intent)

        } else {

            val activities = GROUPS.getOrPut(group) {

                HashSet()
            }

            fun addToGroups() {

                clazz().simpleName.let { name ->

                    activities.add(name)
                    GROUPS[group] = activities
                }
            }

            if (activities.isEmpty()) {

                addToGroups()

                val parentIntent = Intent(this, backgroundActivity)

                parentIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

                doStartActivity(parentIntent) {

                    doStartActivity(intent)
                }

            } else {

                addToGroups()

                doStartActivity(intent)
            }
        }

        if (transition == null) {

            doStartActivity(intent)
        }
    }

    override fun onDestroy() {

        val group = getGroup()

        if (group.isNotEmpty()) {

            val activities = GROUPS.getOrPut(group) {

                HashSet()
            }

            clazz().simpleName.let { name ->

                activities.remove(name)
                GROUPS[group] = activities
            }

            if (activities.isEmpty()) {

                LocalBroadcastManager.getInstance(this).sendBroadcast(

                    Intent(Broadcast.ACTION_FINISH_BY_ACTIVITY_CLASS).apply {

                        putExtra(

                            Broadcast.EXTRA_ACTIVITY_CLASS,
                            backgroundActivity.name
                        )
                    }
                )

                val duration =
                    (resources.getInteger(R.integer.transition_effect_duration) * 1.5).toLong()

                exec(

                    delayInMilliseconds = duration

                ) {

                    super.onDestroy()
                }

            } else {

                super.onDestroy()
            }

        } else {

            super.onDestroy()
        }
    }

    override fun finish() {

        super.finish()

        applyExitTransition("finish")
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        Console.log("$tag onCreate")

        window.setBackgroundDrawable(background.toDrawable())

        super.onCreate(savedInstanceState)
    }

    override fun onPause() {
        super.onPause()

        if (!isFinishing) {

            applyEnterTransition("onPause")
        }
    }

    override fun onResume() {
        super.onResume()

        applyEnterTransition("onResume")
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun overridePendingTransition(enterAnim: Int, exitAnim: Int, backgroundColor: Int) {

        if (enterAnim == 0 && exitAnim == 0) {

            val tag = "$tag Do override pending transition (with background) with no transition ::"

            if (hasTransitionAssigned("overridePendingTransition")) {

                Console.log("$tag SKIPPED")

            } else {

                Console.warning("$tag APPLIED")

                try {

                    super.overridePendingTransition(enterAnim, exitAnim, backgroundColor)

                } catch (_: Throwable) {

                    super.overridePendingTransition(enterAnim, exitAnim)
                }
            }

        } else {

            Console.log("$tag  Do override pending transition (with background)")

            try {

                super.overridePendingTransition(enterAnim, exitAnim, backgroundColor)

            } catch (_: Throwable) {

                super.overridePendingTransition(enterAnim, exitAnim)
            }
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

                Console.warning("$tag APPLIED")

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

    private fun getTransitionAnnotation(
        from: String,
        clazz: Class<*> = this::class.java
    ): TransitionEffects? {

        val tag = "$tag Get annotation :: From='$from' ::"

        Console.log("$tag  START")

        val result = transitionCache.getOrPut(clazz) {

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

    private fun clazz() = this@TransitionEffectsActivity::class.java

    private fun doStartActivity(intent: Intent, callback: (() -> Unit)? = null) {

        applyExitTransition("startActivity")

        fun next() {

            try {

                super.startActivity(intent)

                callback?.let {

                    it()
                }

            } catch (e: Throwable) {

                recordException(e)
            }
        }

        if (hasTransitionAssigned("startActivity")) {

            val duration =
                (resources.getInteger(R.integer.transition_effect_duration) * 1.5).toLong()

            exec(

                delayInMilliseconds = duration

            ) {

                next()
            }

        } else {

            next()
        }
    }

    private fun getGroup(intent: Intent? = null): String {

        var group = ""
        var transition = getTransitionAnnotation("getGroup")

        transition?.let {

            group = it.group
        }

        val component = intent?.component

        component?.let { c ->

            try {

                val targetClass: Class<*> = Class.forName(c.className)

                transition = getTransitionAnnotation("startActivity", targetClass)

                transition?.let {

                    if (it.group.isNotEmpty()) {

                        group = it.group
                    }
                }

            } catch (e: Throwable) {

                recordException(e)
            }
        }

        return group
    }
}