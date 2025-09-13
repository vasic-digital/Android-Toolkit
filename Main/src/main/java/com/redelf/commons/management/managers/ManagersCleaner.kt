package com.redelf.commons.management.managers

import com.redelf.commons.extensions.CountDownLatch
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.management.DataManagement
import com.redelf.commons.management.Management
import com.redelf.commons.obtain.OnObtain
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

class ManagersCleaner {

    interface CleanupCallback {

        fun onCleanup(success: Boolean, error: Throwable? = null)

        fun onCleanup(manager: Management, success: Boolean, error: Throwable? = null)
    }

    fun cleanupManagers(managers: List<Management>): Boolean {

        val latch = CountDownLatch(1, "ManagersCleaner.cleanupManagers")
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

        var managersListLog = ""

        managers.forEachIndexed { index, it ->

            managersListLog += "${it.getWho()}"

            if (index < managers.size - 1) {

                managersListLog += ", "
            }
        }

        Console.log("$tag START: $managersListLog$")

        val success = AtomicBoolean(true)
        val latch = CountDownLatch(managers.size, "ManagersCleaner.cleanupManagers.2")

        exec(

            onRejected = { e ->

                recordException(e)
                latch.countDown()
            }

        ) {

            managers.forEach { manager ->

                Console.log("$tag Manager :: ${manager.getWho()}")

                if (manager is DataManagement<*>) {

                    manager.lock()

                    Console.log(

                        "$tag Manager :: ${manager.getWho()} :: LOCKED"
                    )

                    manager.reset(

                        "cleanupManagers",

                        object : OnObtain<Boolean?> {

                            override fun onCompleted(data: Boolean?) {

                                if (data == true) {

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

                                latch.countDown()
                            }

                            override fun onFailure(error: Throwable) {

                                recordException(error)
                                latch.countDown()
                            }
                        }
                    )

                } else {

                    success.set(false)

                    Console.warning(

                        "$tag Manager :: ${manager.getWho()} :: " +
                                "SKIPPED: Not data manager"
                    )

                    latch.countDown()
                }
            }
        }

        if (!latch.await(5, TimeUnit.SECONDS)) {

            val e = TimeoutException("Timed-out waiting for managers to clean up")
            recordException(e)
            success.set(false)
        }

        callback.onCleanup(success.get())
    }
}