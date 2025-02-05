package com.redelf.analytics.exception

class AnalyticsParametersCountException(expected: Int = 3) : AnalyticsIllegalArgumentsException(

    "Firebase analytics parameters must be at least $expected"
)