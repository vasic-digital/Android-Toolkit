package com.redelf.analytics.implementation.facebook

import com.redelf.analytics.Analytics
import com.redelf.analytics.AnalyticsParameter

class FacebookAnalytics : Analytics {

    override fun log(vararg params: AnalyticsParameter<*>) {

        if (params.size < 3) {

            throw IllegalArgumentException("Firebase analytics parameters must be at least 3")
        }

        TODO("Not yet implemented")
    }
}