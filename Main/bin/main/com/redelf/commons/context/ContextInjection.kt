package com.redelf.commons.context

import android.content.Context

interface ContextInjection<T : Context> {

    fun injectContext(ctx: T)
}