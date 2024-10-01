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

        if (params.size < 3) {

            throw AnalyticsParametersCountException()
        }

        val bundle = Bundle()

        val name = params[0]?.obtain() as String?
        val key = params[1]?.obtain() as String?
        val value = params[2]?.obtain() as String?

        name?.let {
            key?.let {
                value?.let {

                    val analyticEvent = FirebaseAnalyticsEvent(name = name, param = Pair(key, value))

                    val paramLog = "Name = $name, Bundle :: Key: = '${analyticEvent.param?.first}', " +
                            "Value = '${analyticEvent.param?.second}'"

                    analyticEvent.param?.let {

                        bundle.putString(analyticEvent.param.first, analyticEvent.param.second)
                    }

                    exec(

                        onRejected = { e -> recordException(e) }

                    ) {

                        Firebase.analytics.logEvent(analyticEvent.name, bundle)

                        Console.log("$tag Logged event :: $paramLog")
                    }
                }
            }
        }

        if (name == null || key == null || value == null) {

            throw AnalyticsNullParameterException()
        }
    }
}