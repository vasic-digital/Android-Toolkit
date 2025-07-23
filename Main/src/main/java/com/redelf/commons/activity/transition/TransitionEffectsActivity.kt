@file:Suppress("DEPRECATION")

package com.redelf.commons.activity.transition

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.redelf.commons.R
import com.redelf.commons.activity.base.BaseDialogFragment
import com.redelf.commons.activity.stateful.StatefulActivity
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.getAnimationResource
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.messaging.broadcast.Broadcast
import java.lang.annotation.Inherited
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet


abstract class TransitionEffectsActivity : AppCompatActivity() {

    // TODO: Support multiple instances stacked of the same activity

    // TODO: Pass the background parameter to the Background class activity that we will start
    protected open val background = Color.WHITE
    protected open val backgroundActivity = BackgroundActivity::class.java

    private var expectedFinish = false
    private lateinit var backPressedCallback: OnBackPressedCallback
    private val tag = "Transition effects :: ${this::class.simpleName} :: ${this.hashCode()} ::"

    companion object {

        private var GROUPS_PARENT: Class<*>? = null
        private val GROUPS = ConcurrentHashMap<String, CopyOnWriteArraySet<String>>()
        private val GROUPS_BACKGROUND_ACTIVITIES = ConcurrentHashMap<String, Class<*>>()

        private val transitionCache = mutableMapOf<Class<*>, TransitionEffects?>()
    }

    open fun onBack() {

        val tag = "On back ::"

        Console.log("$tag START")

        if (isFinishing) {

            Console.warning("$tag SKIPPED")

            return
        }

        Console.log("$tag END")

        finishFrom("onBack")
    }

    open fun finishFrom(from: String) {

        val tag = "Finish :: Activity='${this.javaClass.simpleName}' :: From='$from'"

        Console.log("$tag START")

        finish()

        Console.log("$tag END")
    }

    fun showInActivity(activity: Class<*>, what: BaseDialogFragment) {

        if (!what.showInActivity(activity, this)) {

            val clazz = what::class.simpleName
            val msg = "'$clazz' was not shown in activity"
            val e = IllegalStateException(msg)

            recordException(e)
        }
    }

    /*
    *   TODO: Implement groups support for the nested groups and multiple instances of the activity
    */
    override fun startActivity(intent: Intent) {

        var checked = false
        val component = intent.component
        val tag = "$tag Start activity ::"

        component?.let { c ->

            try {

                val targetClass: Class<*> = Class.forName(c.className)
                val isToolkitActivity = StatefulActivity::class.java.isAssignableFrom(targetClass)

                checked = isToolkitActivity

                if (!isToolkitActivity) {

                    Console.warning(

                        "$tag Not a toolkit activity :: Clazz='${targetClass.simpleName}'"
                    )
                }

            } catch (e: Throwable) {

                recordException(e)
            }
        }

        if (!checked) {

            doStartActivity("startActivity.notChecked", intent)

            Console.log("$tag SKIPPED")

            return
        }

        Console.log("$tag START")

        val group = getGroup(intent)
        val transition = getTransitionAnnotation("startActivity")

        if (group.isEmpty()) {

            doStartActivity("startActivity.group.empty", intent)

        } else {

            val activities = GROUPS.getOrPut(group) {

                CopyOnWriteArraySet()
            }

            fun addToGroups() {

                val component = intent.component

                component?.let { c ->

                    try {

                        val targetClass: Class<*> = Class.forName(c.className)

                        targetClass.simpleName.let { name ->

                            activities.add(name)
                            GROUPS[group] = activities
                        }

                    } catch (e: Throwable) {

                        recordException(e)
                    }
                }
            }

            if (activities.isEmpty()) {

                addToGroups()

                if (!hasTransitionAssigned("startActivity")) {

                    GROUPS_PARENT = clazz()

                    Console.debug("$tag Groups parent :: Set :: Parent='${GROUPS_PARENT?.simpleName}'")
                }

                val active = BaseApplication.takeContext().activityTracker.isActivityInStack(

                    activityClass = backgroundActivity
                )

                if (active) {

                    doStartActivity("startActivity.active", intent)

                } else {

                    if (GROUPS_BACKGROUND_ACTIVITIES[group] == null) {

                        Console.log("$tag Background activity :: Starting")

                        GROUPS_BACKGROUND_ACTIVITIES[group] = backgroundActivity

                        val parentIntent = Intent(

                            applicationContext,
                            GROUPS_BACKGROUND_ACTIVITIES[group]
                        )

                        parentIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

                        val hold = R.anim.hold

                        overridePendingTransition(hold, hold)

                        doStartActivity("startActivity.notActive.noGroup.parent", parentIntent) {

                            val duration =
                                (resources.getInteger(R.integer.transition_effect_duration)).toLong()

                            exec(

                                delayInMilliseconds = duration

                            ) {

                                doStartActivity("startActivity.notActive.noGroup.child", intent)
                            }
                        }

                    } else {

                        Console.log("$tag Background activity :: Skipping")

                        doStartActivity("startActivity.notActive.group", intent)
                    }
                }

            } else {

                addToGroups()

                doStartActivity("startActivity.activities.notEmpty", intent)
            }
        }

        if (transition == null) {

            doStartActivity("startActivity.noTransition", intent)
        }
    }

    override fun finish() {

        val tag = "$tag Finish ::"

        Console.log("$tag START")

        if (isFinishing) {

            Console.warning("$tag SKIPPED")

            return
        }

        fun next(from: String) {

            val tag = "$tag Next :: From='$from' ::"

            Console.log("$tag ENDING")

            super.finish()

            applyExitTransition("finish")

            Console.log("$tag ENDED")
        }

        val group = getGroup()

        if (group.isNotEmpty()) {

            val activities = GROUPS.getOrPut(group) {

                CopyOnWriteArraySet()
            }

            clazz().simpleName.let { name ->

                activities.remove(name)
                GROUPS[group] = activities
            }

            if (activities.isEmpty()) {

                if (!expectedFinish) {

                    GROUPS_PARENT?.let {

                        val parent = it

                        GROUPS_PARENT = null

                        Console.debug("$tag Groups parent :: Cleared :: Parent='null'")

                        val intent = Intent(

                            applicationContext,
                            parent
                        )

                        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

                        Console.log(

                            "$tag Groups parent :: Starting :: Parent='${parent.simpleName}'"
                        )

                        doStartActivity("finish.hasParent", intent)
                    }
                }

                val duration =
                    (resources.getInteger(R.integer.transition_effect_duration) * 1.5).toLong()

                next("finish.activities.empty.(expectedFinish=$expectedFinish)")

                if (!expectedFinish) {

                    exec(

                        delayInMilliseconds = duration

                    ) {

                        Console.log("$tag Background activity :: Going to finish")

                        val sent = LocalBroadcastManager.getInstance(this).sendBroadcast(

                            Intent(Broadcast.ACTION_FINISH_BY_ACTIVITY_CLASS).apply {

                                putExtra(

                                    Broadcast.EXTRA_ACTIVITY_CLASS,
                                    backgroundActivity.name
                                )
                            }
                        )

                        if (sent) {

                            Console.log("$tag Background activity :: Finish scheduled")

                            val cleared = GROUPS_BACKGROUND_ACTIVITIES.remove(group)

                            cleared?.let {

                                Console.log("$tag Background activity :: Cleared")
                            }

                            if (cleared == null) {

                                Console.error("$tag Background activity :: Not cleared")
                            }

                        } else {

                            Console.error(

                                "$tag Background activity :: Failed to schedule finish"
                            )
                        }
                    }
                }

            } else {

                next("finish.activities.notEmpty")
            }

        } else {

            next("finish.noGroup")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        Console.log("$tag onCreate")

        super.onCreate(savedInstanceState)

        window.setTransitionBackgroundFadeDuration(0)
        window.setBackgroundDrawable(background.toDrawable())

        backPressedCallback = object : OnBackPressedCallback(true) {

            override fun handleOnBackPressed() {

                onBack()
            }
        }

        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    override fun onPause() {
        super.onPause()

        if (!isFinishing) {

            applyEnterTransition("onPause")
        }
    }

    override fun onResume() {

        Console.log("$tag onResume :: ${this::class.simpleName} :: ${hashCode()}")

        super.onResume()

        applyEnterTransition("onResume")
    }

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

            Console.log("$tag Do override pending transition (with background)")

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

    protected fun startActivity(from: String, intent: Intent, callback: () -> Unit) {

        doStartActivity("startActivity.withCallback.(from='$from')", intent, callback)
    }

    protected fun startActivityAndFinish(intent: Intent, from: String) {

        expectedFinish = true

        finishFrom("startActivityAndFinish(from='$from')")

        startActivity(intent)
    }

    @Suppress("DEPRECATION")
    private fun applyEnterTransition(from: String) {

        val tag = "$tag Apply transition :: Enter :: From='$from' ::"

        Console.log("$tag  START")

        val transition = getTransitionAnnotation("applyEnterTransition(from='$from')")

        transition?.let {

            val enter = getAnimationResource(it.enter)

            if (enter > 0) {

                val hold = R.anim.hold

                overridePendingTransition(enter, hold, background)

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

                val hold = R.anim.hold

                overridePendingTransition(hold, exit, background)

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

    private fun doStartActivity(from: String, intent: Intent, callback: (() -> Unit)? = null) {

        var whichOne = "Unknown"
        val component = intent.component

        component?.let { c ->

            try {

                val targetClass: Class<*> = Class.forName(c.className)

                whichOne = targetClass.simpleName ?: whichOne

            } catch (e: Throwable) {

                recordException(e)
            }
        }

        val tag = "$tag Do start activity :: Which='$whichOne', From='$from'"

        Console.log("$tag START")

        applyExitTransition("startActivity")

        fun next() {

            try {

                super.startActivity(intent)

                callback?.let {

                    it()
                }

                Console.log("$tag END")

            } catch (e: Throwable) {

                Console.error("$tag END :: Error='${e.message}'")

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