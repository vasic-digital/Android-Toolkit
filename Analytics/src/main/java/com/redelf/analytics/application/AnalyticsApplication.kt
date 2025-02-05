package com.redelf.analytics.application

import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.logging.Console

abstract class AnalyticsApplication : BaseApplication() {

    protected open val facebookAnalyticsEnabled = true

    override val firebaseAnalyticsEnabled = true

    override fun initFirebaseWithAnalytics() {

        Console.log("Analytics :: Init :: START")

        super.initFirebaseWithAnalytics()

        Console.log("Analytics :: Init :: END")

        if (DEBUG.get() && facebookAnalyticsEnabled) {

            FacebookSdk.setIsDebugEnabled(true)
            FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
        }
    }
}