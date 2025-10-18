package com.redelf.analytics.implementation.facebook

import android.os.Bundle
import com.facebook.appevents.AppEventsLogger
import com.redelf.analytics.Analytics
import com.redelf.analytics.AnalyticsParameter
import com.redelf.analytics.exception.AnalyticsNullParameterException
import com.redelf.analytics.exception.AnalyticsParametersCountException
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console

class FacebookAnalytics : Analytics {

    private val tag = "Analytics :: Facebook ::"

    @Throws(IllegalArgumentException::class)
    override fun log(vararg params: AnalyticsParameter<*>?) {

        if (params.isEmpty()) {

            throw AnalyticsParametersCountException(1)
        }

        val ctx = BaseApplication.takeContext()
        val logger = AppEventsLogger.newLogger(ctx)

        val key = params[0]?.obtain() as String?
        val value = params[1]?.obtain() as Pair<*, *>?

        key?.let {

            exec {

                if (value != null) {

                    if (value!!.first is String && value!!.second is String) {

                        val first = value!!.first as String
                        val second = value!!.second as String

                        val bundle = Bundle()

                        bundle.putString(first, second)

                        logger.logEvent(key, bundle)

                    } else {

                        val e = IllegalArgumentException("Value must be a Pair<String, String>")
                        recordException(e)
                    }
                }

                var paramLog = "Bundle :: Key = '$key', Value = '$value'"

                if (value == null) {

                    paramLog = "Bundle :: Key = '$key'"

                    logger.logEvent(key)
                }

                Console.log("$tag Logged event :: $paramLog")
            }
        }

        if (key == null) {

            throw AnalyticsNullParameterException()
        }
    }
}