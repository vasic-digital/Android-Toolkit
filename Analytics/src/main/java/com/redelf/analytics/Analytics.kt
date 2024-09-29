package com.redelf.analytics

interface Analytics {

    fun log(vararg params: AnalyticsParameter<*>)
}