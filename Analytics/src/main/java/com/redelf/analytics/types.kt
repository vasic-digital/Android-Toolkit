package com.redelf.analytics

import java.util.concurrent.ConcurrentHashMap

typealias AnalyticsParameters = ConcurrentHashMap<AnalyticsArgument, AnalyticsParameter<*>>

fun String.toAnalyticsParameter(): AnalyticsParameter<*> {

    val self = this

    return object : AnalyticsParameter<String> {

        override fun obtain(): String = self
    }
}