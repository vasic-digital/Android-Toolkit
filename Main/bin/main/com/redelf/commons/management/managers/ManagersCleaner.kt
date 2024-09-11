package com.redelf.commons.management.managers

import com.redelf.commons.extensions.exec
import com.redelf.commons.logging.Console
import com.redelf.commons.management.DataManagement
import com.redelf.commons.management.Management
import java.util.concurrent.CountDownLatch
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

class ManagersCleaner {

    interface CleanupCallback {

        fun onCleanup(success: Boolean, error: Throwable? = null)

        fun onCleanup(manager: Management, success: Boolean, error: Throwable? = null)
    }

    fun cleanupManagers(managers: List<Management>): Boolean {

        val latch = CountDownLatch(1)
        val result = AtomicBoolean(true)

        val callback = object : CleanupCallback {

            override fun onCleanup(success: Boolean, error: Throwable?) {

                result.set(success)

                error?.let {

                    Console.error(it)
                }

                latch.countDown()
            }

            override fun onCleanup(manager: Management, success: Boolean, error: Throwable?) {

                error?.let {

                    Console.error(it)
                }

                latch.countDown()
            }
        }

        cleanupManagers(managers, callback)

        latch.await()

        return result.get()
    }

    fun cleanupManagers(managers: List<Management>, callback: CleanupCallback) {

        val tag = "Managers :: Cleanup ::"

        try {

            var managersListLog = ""

            managers.forEachIndexed { index, it ->

                managersListLog += "${it.getWho()}"

                if (index < managers.size - 1) {

                    managersListLog += ", "
                }
            }

            Console.log("$tag START: $managersListLog$")

            exec {

                val success = AtomicBoolean(true)
                val latch = CountDownLatch(managers.size)

                managers.forEach { manager ->

                    try {

                        exec {

                            Console.log("$tag Manager :: ${manager.getWho()}")

                            if (manager is DataManagement<*>) {

                                manager.lock()

                                Console.log(

                                    "$tag Manager :: ${manager.getWho()} :: LOCKED"
                                )

                                if (manager.reset()) {

                                    Console.log(

                                        "$tag Manager :: ${manager.getWho()} :: " +
                                                "Cleaned"
                                    )

                                } else {

                                    Console.warning(

                                        "$tag Manager :: ${manager.getWho()} :: " +
                                                "Not cleaned, not data manager"
                                    )

                                    success.set(false)
                                }

                                manager.unlock()

                                Console.log(

                                    "$tag Manager :: ${manager.getWho()} :: UNLOCKED"
                                )

                            } else {

                                Console.warning(

                                    "$tag Manager :: ${manager.getWho()} :: " +
                                            "SKIPPED: Not data manager"
                                )
                            }

                            latch.countDown()
                        }

                    } catch (e: RejectedExecutionException) {

                        success.set(false)

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

    fun cleanupDataManagers(

        managers: List<DataManagement<*>>,
        callback: CleanupCallback,

        ) {

        val tag = "Managers :: Cleanup ::"

        try {

            Console.log("$tag START")

            exec {

                val success = AtomicBoolean(true)
                val latch = CountDownLatch(managers.size)

                managers.forEach { manager ->

                    Console.log("$tag Manager :: ${manager.getWho()}")

                    exec {

                        if (manager.reset()) {

                            Console.log(

                                "$tag Manager :: ${manager.getWho()} :: " +
                                        "Cleaned"
                            )

                        } else {

                            Console.warning(

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