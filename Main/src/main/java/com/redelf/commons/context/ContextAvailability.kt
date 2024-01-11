package com.redelf.commons.context

import android.content.Context

interface ContextAvailability {

    fun takeContext(): Context
}