package com.redelf.analytics

import com.redelf.commons.sending.Sending

class AnalyticsBuilder(private val backend: Analytics) : Sending {

    private var parameters: AnalyticsParameters = AnalyticsParameters()


    @Throws(IllegalArgumentException::class)
    fun event(value: String): AnalyticsBuilder {

        return map(AnalyticsArgument.EVENT, value.toAnalyticsParameter())
    }

    @Throws(IllegalArgumentException::class)
    fun value(value: String): AnalyticsBuilder {

        return map(AnalyticsArgument.VALUE, value.toAnalyticsParameter())
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

    @Throws(IllegalArgumentException::class)
    fun stringPair(value: Pair<String, String>): AnalyticsBuilder {

        return pair(

            Pair(value.first, value.second.toAnalyticsParameter())
        )
    }

    @Throws(IllegalArgumentException::class)
    fun pair(value: Pair<String, AnalyticsParameter<*>>?): AnalyticsBuilder {

        return mapPair(AnalyticsArgument.PAIR, value)
    }

    @Throws(IllegalArgumentException::class)
    fun listPair(values: Pair<String, List<AnalyticsParameter<*>>?>?): AnalyticsBuilder {

        return mapListPair(AnalyticsArgument.PAIR, values)
    }


    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    override fun send() {

        val pair = parameters[AnalyticsArgument.PAIR]
        val value = parameters[AnalyticsArgument.VALUE]
        val event = parameters[AnalyticsArgument.EVENT]
        val multiple = parameters[AnalyticsArgument.MULTIPLE]

        backend.log(event, value, multiple, pair)
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

    @Throws(IllegalArgumentException::class)
    private fun mapPair(

        key: AnalyticsArgument?,
        pair: Pair<String, AnalyticsParameter<*>>?

    ): AnalyticsBuilder {

        key?.let { k ->
            pair?.let { v ->

                parameters[k] = object : AnalyticsParameter<Pair<String, AnalyticsParameter<*>>?> {

                    override fun obtain(): Pair<String, AnalyticsParameter<*>>? {

                        return v
                    }
                }
            }
        }

        if (key == null || pair == null) {

            throw IllegalArgumentException("Key and Pair parameters are required")
        }

        return this
    }

    @Throws(IllegalArgumentException::class)
    private fun mapListPair(

        key: AnalyticsArgument?,
        pair: Pair<String, List<AnalyticsParameter<*>?>?>?

    ): AnalyticsBuilder {

        key?.let { k ->
            pair?.let { v ->

                parameters[k] =
                    object : AnalyticsParameter<Pair<String, List<AnalyticsParameter<*>?>?>?> {

                        override fun obtain(): Pair<String, List<AnalyticsParameter<*>?>?>? {

                            return v
                        }
                    }
            }
        }

        if (key == null || pair == null) {

            throw IllegalArgumentException("Key and Pair parameters are required")
        }

        return this
    }
}