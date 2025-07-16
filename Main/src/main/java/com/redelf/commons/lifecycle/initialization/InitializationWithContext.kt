package com.redelf.commons.lifecycle.initialization

import android.content.Context

interface InitializationWithContext {

    fun initialize(ctx: Context)
}