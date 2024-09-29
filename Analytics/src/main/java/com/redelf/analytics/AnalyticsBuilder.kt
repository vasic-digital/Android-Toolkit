package com.redelf.analytics

import com.redelf.commons.sending.Sending

class AnalyticsBuilder(private val backend: Analytics) : Sending {

    private var parameters: AnalyticsParameters = AnalyticsParameters()

    @Throws(IllegalArgumentException::class)
    fun category(value: AnalyticsParameter<*>?): AnalyticsBuilder {

        return map(AnalyticsArgument.CATEGORY, value)
    }

    @Throws(IllegalArgumentException::class)
    fun event(value: AnalyticsParameter<*>?): AnalyticsBuilder {

        return map(AnalyticsArgument.EVENT, value)
    }

    @Throws(IllegalArgumentException::class)
    fun value(value: AnalyticsParameter<*>?): AnalyticsBuilder {

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

    @Throws(IllegalArgumentException::class)
    private fun map(key: AnalyticsArgument?, value: AnalyticsParameter<*>?): AnalyticsBuilder {

        key?.let { k ->
            value?.let { v ->

                parameters[k] = v
            }
        }

        if (key == null || value == null) {

            throw IllegalArgumentException("Key and Value parameters are required")
        }

        return this
    }
}