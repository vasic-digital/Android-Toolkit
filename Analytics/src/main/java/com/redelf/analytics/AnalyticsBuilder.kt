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
    fun multiple(vararg values: AnalyticsParameter<*>?): AnalyticsBuilder {

        return map(AnalyticsArgument.MULTIPLE, *values)
    }

    @Throws(IllegalArgumentException::class)
    fun multiple(values: List<AnalyticsParameter<*>?>?): AnalyticsBuilder {

        return map(AnalyticsArgument.MULTIPLE, values)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    override fun send() {

        val value = parameters[AnalyticsArgument.VALUE]
        val event = parameters[AnalyticsArgument.EVENT]
        val category = parameters[AnalyticsArgument.CATEGORY]
        val multiple = parameters[AnalyticsArgument.MULTIPLE]

        backend.log(category, event, value, multiple)
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

    @Throws(IllegalArgumentException::class)
    private fun map(

        key: AnalyticsArgument?,
        values: List<AnalyticsParameter<*>?>?

    ): AnalyticsBuilder {

        key?.let { k ->
            values?.let { v ->

                parameters[k] = object : AnalyticsParameter<List<AnalyticsParameter<*>?>> {

                    override fun obtain(): List<AnalyticsParameter<*>?> {

                        return v
                    }
                }
            }
        }

        if (key == null || values == null) {

            throw IllegalArgumentException("Key and Values parameters are required")
        }

        return this
    }

    @Throws(IllegalArgumentException::class)
    private fun map(

        key: AnalyticsArgument?,
        vararg values: AnalyticsParameter<*>?

    ): AnalyticsBuilder {

        key?.let { k ->
            values.let { v ->

                parameters[k] = object : AnalyticsParameter<List<AnalyticsParameter<*>?>> {

                    override fun obtain(): List<AnalyticsParameter<*>?> {

                        return v.toList()
                    }
                }
            }
        }

        if (key == null) {

            throw IllegalArgumentException("Key and Values parameters are required")
        }

        return this
    }
}