package com.redelf.commons.activity.tracking

import android.app.Activity
import android.app.Application
import android.os.Bundle

class ActivityTracker : Application.ActivityLifecycleCallbacks {

    private val activeActivities = mutableSetOf<String>()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

        activeActivities.add(activity::class.java.name)
    }

    override fun onActivityDestroyed(activity: Activity) {

        activeActivities.remove(activity::class.java.name)
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    fun isActivityInStack(activityClass: Class<*>): Boolean {

        return activeActivities.contains(activityClass.name)
    }
}