package com.redelf.analytics.implementation.firebase

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.redelf.analytics.Analytics
import com.redelf.analytics.AnalyticsParameter
import com.redelf.analytics.exception.AnalyticsNullParameterException
import com.redelf.analytics.exception.AnalyticsParametersCountException
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console

class FirebaseAnalytics : Analytics {

    private val tag = "Analytics :: Firebase ::"

    @Throws(IllegalArgumentException::class)
    override fun log(vararg params: AnalyticsParameter<*>?) {

        if (params.isEmpty()) {

            throw AnalyticsParametersCountException(1)
        }

        val bundle = Bundle()

        val key = params[0]?.obtain() as String?
        val value = params[1]?.obtain() as String?

        key?.let {

            val analyticEvent = FirebaseAnalyticsEvent(param = Pair(key, value ?: ""))

            val paramLog = "Bundle :: Key: = '${analyticEvent.param?.first}', " +
                    "Value = '${analyticEvent.param?.second}'"

            analyticEvent.param?.let {

                bundle.putString(analyticEvent.param.first, analyticEvent.param.second)
            }

            exec(

                onRejected = { e -> recordException(e) }

            ) {

                Firebase.analytics.logEvent(key, bundle)

                Console.log("$tag Logged event :: $paramLog")
            }
        }

        if (key == null) {

            throw AnalyticsNullParameterException()
        }
    }
}