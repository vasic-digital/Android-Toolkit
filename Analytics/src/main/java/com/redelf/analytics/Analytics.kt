package com.redelf.analytics

interface Analytics {

    companion object {

        fun build(backend: Analytics): AnalyticsBuilder {

            return AnalyticsBuilder(backend)
        }
    }

    fun log(vararg params: AnalyticsParameter<*>?)
}