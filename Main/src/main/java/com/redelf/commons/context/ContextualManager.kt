package com.redelf.commons.context

import android.content.Context
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.management.DataManagement

abstract class ContextualManager<T> : DataManagement<T>(), Contextual {

    private lateinit var ctx: Context

    override fun takeContext(): Context {

        if (!this::ctx.isInitialized) {

            ctx = BaseApplication.CONTEXT
        }

        return ctx
    }

    @Synchronized
    override fun injectContext(ctx: Context) {

        this@ContextualManager.ctx = ctx.applicationContext
    }
}