package com.redelf.commons.context

import android.content.Context

interface ContextAvailability<T : Context> {

    fun takeContext(): T
}