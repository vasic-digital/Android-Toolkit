package com.redelf.commons.security.cleanup

import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.execution.Executor
import com.redelf.commons.persistance.Data
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class ApplicationCleanup {

    private val cleaningUp = AtomicBoolean()

    private val callbacks =
        Callbacks<ApplicationCleanupCallback>(identifier = "Application cleanup")

    private val cleanupAction = Runnable {

        val success = Data.deleteAll()
        if (success) {
            Timber.i("Application has been cleaned up")
        } else {
            Timber.w("Could not clean up the application")
        }
        callbacks.doOnAll(

            object : CallbackOperation<ApplicationCleanupCallback> {
                override fun perform(callback: ApplicationCleanupCallback) {

                    callback.onCleanup(success)
                    callbacks.unregister(callback)
                }
            },
            "Cleanup action"
        )
        cleaningUp.set(false)
    }

    fun cleanup(callback: ApplicationCleanupCallback) {

        callbacks.register(callback)
        if (cleaningUp.get()) {

            Timber.w("Already cleaning up the application")
            return
        }
        cleaningUp.set(true)
        Executor.MAIN.execute(cleanupAction)
    }
}