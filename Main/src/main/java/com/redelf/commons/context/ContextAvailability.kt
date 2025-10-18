package com.redelf.commons.context

import android.content.Context

fun interface ContextAvailability<T : Context> {

    fun takeContext(): T
}