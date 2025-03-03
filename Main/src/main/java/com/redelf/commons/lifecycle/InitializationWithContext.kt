package com.redelf.commons.lifecycle

import android.content.Context

interface InitializationWithContext {

    fun initialize(ctx: Context)
}