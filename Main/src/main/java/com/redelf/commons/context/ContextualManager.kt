package com.redelf.commons.context

import com.redelf.commons.application.BaseApplication
import com.redelf.commons.management.DataManagement

abstract class ContextualManager<T> : DataManagement<T>(), Contextual<BaseApplication> {

    private lateinit var ctx: BaseApplication

    override fun takeContext(): BaseApplication {

        if (!this::ctx.isInitialized) {

            ctx = BaseApplication.CONTEXT
        }

        return ctx
    }

    @Synchronized
    override fun injectContext(ctx: BaseApplication) {

        this@ContextualManager.ctx = ctx
    }
}