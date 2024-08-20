package com.redelf.commons.interprocess

import com.redelf.commons.application.BaseApplication

abstract class InterprocessApplication : BaseApplication() {

    protected abstract fun getProcessors(): List<InterprocessProcessor>

    override fun onDoCreate() {
        super.onDoCreate()

        getProcessors().forEach { processor -> Interprocessor.register(processor) }
    }
}