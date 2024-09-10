package com.redelf.commons.interprocess

import com.redelf.commons.application.BaseApplication
import com.redelf.commons.logging.Console

abstract class InterprocessApplication : BaseApplication() {

    protected abstract val interprocessPermission: Int

    protected abstract fun getProcessors(): List<InterprocessProcessor>

    override fun onDoCreate() {
        super.onDoCreate()

        getProcessors().forEach { processor -> Interprocessor.register(processor) }

        Console.log(

            "IPC :: Permission :: ${getString(interprocessPermission)}"
        )
    }

    protected fun getInterprocessPermissionValue() =
        applicationContext.getString(interprocessPermission)
}