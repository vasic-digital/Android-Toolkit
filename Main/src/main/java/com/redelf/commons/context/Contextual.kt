package com.redelf.commons.context

import android.content.Context

interface Contextual {

    fun takeContext(): Context

    fun injectContext(ctx: Context)
}