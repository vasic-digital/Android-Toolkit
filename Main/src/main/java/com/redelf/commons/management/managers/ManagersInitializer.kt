package com.redelf.commons.management.managers

import com.redelf.commons.application.BaseApplication
import com.redelf.commons.defaults.ResourceDefaults
import com.redelf.commons.extensions.exec
import com.redelf.commons.logging.Console
import com.redelf.commons.management.DataManagement
import com.redelf.commons.management.LazyDataManagement
import com.redelf.commons.management.Management
import java.util.concurrent.CountDownLatch
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

class ManagersInitializer {

    interface InitializationCallback {

        fun onInitialization(success: Boolean, error: Throwable? = null)

        fun onInitialization(manager: Management, success: Boolean, error: Throwable? = null)
    }

    fun initializeManagers(

        managers: List<Management>,
        context: BaseApplication? = null,
        defaultResources: Map<Class<*>, Int>? = null

    ): Boolean {

        val latch = CountDownLatch(1)
        val result = AtomicBoolean(true)

        val callback = object : InitializationCallback {

            override fun onInitialization(success: Boolean, error: Throwable?) {

                if (!success) {

                    result.set(false)
                }

                latch.countDown()
            }

            override fun onInitialization(

                manager: Management,
                success: Boolean,
                error: Throwable?

            ) {

                error?.let {

                    Console.error(it)
                }
            }
        }

        initializeManagers(managers, callback, context, defaultResources)

        latch.await()

        return result.get()
    }

    fun initializeManagers(

        managers: List<Management>,
        callback: InitializationCallback,
        context: BaseApplication? = null,
        defaultResources: Map<Class<*>, Int>? = null

    ) {

        val tag = "Initialize managers ::"

        try {

            exec {

                Console.log("$tag START")

                val failure = AtomicBoolean()

                managers.forEach { manager ->

                    val mTag = "$tag ${manager.getWho()} :: ${manager.hashCode()} ::"

                    exec {

                        if (failure.get()) {

                            Console.error("$mTag initialization skipped")

                        } else {

                            if (manager is DataManagement<*>) {

                                context?.let { ctx ->

                                    if (manager.takeContext() != ctx) {

                                        Console.log(

                                            "$mTag Injecting context: '$ctx'"
                                        )

                                        manager.injectContext(ctx)
                                    }
                                }

                                if (manager is ResourceDefaults) {

                                    val defaultResource = defaultResources?.get(manager.javaClass)
                                    defaultResource?.let {

                                        Console.log(

                                            "$mTag Setting defaults from the " +
                                                    "resource: $defaultResource"
                                        )

                                        manager.setDefaults(it)
                                    }
                                }

                                if (manager is LazyDataManagement<*>) {

                                    context?.let { ctx ->

                                        if (!manager.isRegistered(ctx)) {

                                            val mlTag = "$mTag Registering to app lifecycle events ::"

                                            Console.log("$mlTag START")

                                            Console.log("$mlTag REGISTERING")

                                            manager.register(ctx)

                                            Console.log(

                                                "$mlTag REGISTERED: ${manager.isRegistered(ctx)}"
                                            )
                                        }
                                    }
                                }

                            } else {

                                Console.warning("$mTag Not supported")
                            }
                        }
                    }
                }

                callback.onInitialization(!failure.get())
            }

        } catch (e: RejectedExecutionException) {

            callback.onInitialization(false, e)
        }
    }
}