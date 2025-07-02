package com.redelf.commons.context

import com.redelf.commons.application.BaseApplication
import com.redelf.commons.management.LazyDataManagement
import com.redelf.commons.versioning.Versionable

abstract class ContextualManager<T> :

    LazyDataManagement<T>(),
    Contextual<BaseApplication> where T : Versionable
{

    private lateinit var ctx: BaseApplication

    override fun takeContext(): BaseApplication {

        if (!this::ctx.isInitialized) {

            ctx = BaseApplication.CONTEXT
        }

        return ctx
    }


    override fun injectContext(ctx: BaseApplication) {
        super.injectContext(ctx)

        this@ContextualManager.ctx = ctx
    }
}