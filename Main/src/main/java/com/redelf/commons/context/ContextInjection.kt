package com.redelf.commons.context

import android.content.Context

interface ContextInjection {

    fun injectContext(ctx: Context)
}