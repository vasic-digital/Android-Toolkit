package com.redelf.analytics

import com.redelf.commons.sending.Sending

class AnalyticsBuilder(private val backend: Analytics) : Sending {

    private var parameters: AnalyticsParameters = AnalyticsParameters()

    fun category(value: AnalyticsParameter<*>): AnalyticsBuilder {

        return map(AnalyticsArgument.CATEGORY, value)
    }

    fun event(value: AnalyticsParameter<*>): AnalyticsBuilder {

        return map(AnalyticsArgument.EVENT, value)
    }

    fun value(value: AnalyticsParameter<*>): AnalyticsBuilder {

        return map(AnalyticsArgument.VALUE, value)
    }

    @Throws(IllegalArgumentException::class)
    override fun send() {

        parameters[AnalyticsArgument.CATEGORY]?.let { category ->
            parameters[AnalyticsArgument.EVENT]?.let { event ->
                parameters[AnalyticsArgument.VALUE]?.let { value ->

                    backend.log(category, event, value)

                    return
                }
            }
        }

        throw IllegalArgumentException("Category, Event and Value parameters are required")
    }

    private fun map(key: AnalyticsArgument, value: AnalyticsParameter<*>): AnalyticsBuilder {

        parameters[key] = value

        return this
    }
}