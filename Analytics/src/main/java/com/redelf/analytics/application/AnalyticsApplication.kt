package com.redelf.analytics.application

import com.redelf.commons.application.BaseApplication

abstract class AnalyticsApplication : BaseApplication() {

    protected open val facebookEnabled = true
    protected open val facebookAnalyticsEnabled = true

    override fun initFirebaseWithAnalytics() {
        super.initFirebaseWithAnalytics()


    }
}