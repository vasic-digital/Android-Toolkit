package com.redelf.commons.context

import android.content.Context

fun interface ContextInjection<T : Context> {

    fun injectContext(ctx: T)
}