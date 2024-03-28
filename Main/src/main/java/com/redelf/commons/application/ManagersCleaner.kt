package com.redelf.commons.application

import android.content.Context
import com.redelf.commons.context.Contextual
import com.redelf.commons.defaults.ResourceDefaults
import com.redelf.commons.exec
import com.redelf.commons.management.DataManagement
import com.redelf.commons.management.Management
import com.redelf.commons.persistance.EncryptedPersistence
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

class ManagersCleaner {

    interface CleanupCallback {

        fun onCleanup(success: Boolean, error: Throwable? = null)

        fun onCleanup(manager: Management, success: Boolean, error: Throwable? = null)
    }

    fun cleanupManagers(

        managers: List<Management>,
        callback: CleanupCallback,

    ) {

        try {

            exec {

                val failure = AtomicBoolean()

                managers.forEach { manager ->

                    if (failure.get()) {

                        Timber.e(

                            "Manager: ${manager.javaClass.simpleName} initialization skipped"
                        )

                        return@exec
                    }

                    if (manager is DataManagement<*>) {

                        val latch = CountDownLatch(1)

                        if (!manager.reset()) {

                            failure.set(true)
                        }

                        latch.await()
                    }
                }

                callback.onCleanup(!failure.get())
            }

        } catch (e: RejectedExecutionException) {

            callback.onCleanup(false, e)
        }
    }
}