package com.redelf.analytics.application

import com.redelf.commons.application.BaseApplication
import com.redelf.commons.logging.Console

abstract class AnalyticsApplication : BaseApplication() {

    protected open val facebookEnabled = true
    protected open val facebookAnalyticsEnabled = true

    override val firebaseAnalyticsEnabled = true

    override fun initFirebaseWithAnalytics() {

        Console.log("Analytics :: Init :: START")

        super.initFirebaseWithAnalytics()

        Console.log("Analytics :: Init :: END")
    }
}