package com.redelf.commons.security.check

import com.google.firebase.crashlytics.internal.common.CommonUtils
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.execution.Executor
import com.redelf.commons.logging.Console
import java.util.concurrent.atomic.AtomicBoolean

class SafetyChecks {

    private val checking = AtomicBoolean()
    private val callbacks = Callbacks<SafetyCheckCallback>(identifier = "Safety check")

    private val check = Runnable {

        val isRooted = CommonUtils.isRooted()
        callbacks.doOnAll(

            object : CallbackOperation<SafetyCheckCallback> {
                override fun perform(callback: SafetyCheckCallback) {

                    callback.onRootingCheck(isRooted)
                    callbacks.unregister(callback)
                }
            },
            "Safety check"
        )
        checking.set(false)
    }

    fun checkRooted(callback: SafetyCheckCallback) {

        callbacks.register(callback)
        if (checking.get()) {

            Console.warning("Root check is already in prgress")
            return
        }
        checking.set(true)
        Executor.MAIN.execute(check)
    }
}