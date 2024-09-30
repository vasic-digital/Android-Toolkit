package com.redelf.analytics.implementation.facebook

import android.os.Bundle
import com.redelf.analytics.Analytics
import com.redelf.analytics.AnalyticsParameter

import com.facebook.appevents.AppEventsLogger
import com.redelf.analytics.exception.AnalyticsParametersCountException
import com.redelf.analytics.implementation.firebase.FirebaseAnalyticsEvent
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.logging.Console

class FacebookAnalytics : Analytics {

    private val tag = "Analytics :: Facebook ::"

    @Throws(IllegalArgumentException::class)
    override fun log(vararg params: AnalyticsParameter<*>) {

        if (params.size < 3) {

            throw AnalyticsParametersCountException()
        }

        val bundle = Bundle()
        val ctx = BaseApplication.takeContext()
        val logger = AppEventsLogger.newLogger(ctx)

        val name = params[0].obtain() as String
        val key = params[1].obtain() as String
        val value = params[2].obtain() as String

        val analyticEvent = FirebaseAnalyticsEvent(name = name, param = Pair(key, value))

        val paramLog = "Name = $name, Bundle :: Key: = '${analyticEvent.param?.first}', " +
                "Value = '${analyticEvent.param?.second}'"

        analyticEvent.param?.let {

            bundle.putString(analyticEvent.param.first, analyticEvent.param.second)
        }

        logger.logEvent(name, bundle)

        Console.log("$tag Logged event :: $paramLog")
    }
}