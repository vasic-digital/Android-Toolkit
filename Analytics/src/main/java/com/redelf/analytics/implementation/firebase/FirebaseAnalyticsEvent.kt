package com.redelf.analytics.implementation.firebase

data class FirebaseAnalyticsEvent(val name: String, val param: Pair<String, String>? = null)