package com.redelf.commons.management.managers

import com.redelf.commons.exec
import com.redelf.commons.management.DataManagement
import com.redelf.commons.management.Management
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

        val tag = "Managers :: Cleanup ::"

        try {

            Timber.v("$tag START")

            exec {

                managers.forEach { manager ->

                    if (manager is DataManagement<*>) {

                        manager.lock()

                        Timber.v(

                            "$tag Manager :: ${manager.getWho()} :: LOCKED"
                        )
                    }
                }

                val failure = AtomicBoolean()

                managers.forEach { manager ->

                    Timber.v("$tag Manager :: ${manager.getWho()}")

                    if (manager is DataManagement<*>) {

                        if (manager.reset()) {

                            Timber.v(

                                "$tag Manager :: ${manager.getWho()} :: " +
                                        "Cleaned"
                            )

                        } else {

                            Timber.w(

                                "$tag Manager :: ${manager.getWho()} :: " +
                                        "Not cleaned, not data manager"
                            )

                            failure.set(true)
                        }

                    } else {

                        Timber.w(

                            "$tag Manager :: ${manager.getWho()} :: " +
                                    "SKIPPED: Not data manager"
                        )
                    }
                }

                managers.forEach { manager ->

                    if (manager is DataManagement<*>) {

                        manager.unlock()

                        Timber.v(

                            "$tag Manager :: ${manager.getWho()} :: UNLOCKED"
                        )
                    }
                }

                callback.onCleanup(!failure.get())
            }

        } catch (e: RejectedExecutionException) {

            callback.onCleanup(false, e)
        }
    }

    fun cleanupDataManagers(

        managers: List<DataManagement<*>>,
        callback: CleanupCallback,

        ) {

        val tag = "Managers :: Cleanup ::"

        try {

            Timber.v("$tag START")

            exec {

                val success = AtomicBoolean(true)
                val latch = CountDownLatch(managers.size)

                managers.forEach { manager ->

                    Timber.v("$tag Manager :: ${manager.getWho()}")

                    exec {

                        if (manager.reset()) {

                            Timber.v(

                                "$tag Manager :: ${manager.getWho()} :: " +
                                        "Cleaned"
                            )

                        } else {

                            Timber.w(

                                "$tag Manager :: ${manager.getWho()} :: " +
                                        "Not cleaned"
                            )

                            success.set(false)
                        }

                        latch.countDown()
                    }
                }

                latch.await()

                callback.onCleanup(success.get())
            }

        } catch (e: RejectedExecutionException) {

            callback.onCleanup(false, e)
        }
    }
}