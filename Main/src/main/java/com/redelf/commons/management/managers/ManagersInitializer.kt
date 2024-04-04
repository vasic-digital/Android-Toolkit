package com.redelf.commons.management.managers

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

class ManagersInitializer {

    interface InitializationCallback {

        fun onInitialization(success: Boolean, error: Throwable? = null)

        fun onInitialization(manager: Management, success: Boolean, error: Throwable? = null)
    }

    fun initializeManagers(

        managers: List<Management>,
        persistence: EncryptedPersistence? = null,
        context: Context? = null,
        defaultResources: Map<Class<*>, Int>? = null

    ) : Boolean {

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

                    Timber.e(it)
                }
            }
        }

        initializeManagers(managers, callback, persistence, context, defaultResources)

        latch.await()

        return result.get()
    }

    fun initializeManagers(

        managers: List<Management>,
        callback: InitializationCallback,
        persistence: EncryptedPersistence? = null,
        context: Context? = null,
        defaultResources: Map<Class<*>, Int>? = null

    ) {

        val tag = "Initialize managers ::"

        try {

            exec {

                Timber.v("$tag START")

                val failure = AtomicBoolean()
                val latch = CountDownLatch(managers.size)

                managers.forEach { manager ->

                    val mTag = "$tag ${manager.getWho()} :: ${manager.hashCode()} ::"

                    exec {

                        if (failure.get()) {

                            Timber.e("$mTag initialization skipped")

                            latch.countDown()

                        } else {

                            if (manager is DataManagement<*>) {

                                val lifecycleCallback = object : ManagerLifecycleCallback() {

                                    override fun onInitialization(

                                        success: Boolean,
                                        vararg args: EncryptedPersistence

                                    ) {

                                        if (success) {

                                            Timber.v(

                                                "$mTag " +
                                                        "Initialization completed with success: " +
                                                        "(${manager.isInitialized()})"
                                            )

                                        } else {

                                            Timber.v("$mTag Initialization failed")

                                            failure.set(true)
                                        }

                                        callback.onInitialization(manager, success)

                                        latch.countDown()
                                    }
                                }

                                if (manager.initializationReady()) {

                                    if (manager is Contextual) {

                                        context?.let { ctx ->

                                            Timber.v(

                                                "$mTag Injecting context: $ctx"
                                            )

                                            manager.injectContext(ctx)
                                        }
                                    }

                                    if (manager is ResourceDefaults) {

                                        val defaultResource = defaultResources?.get(manager.javaClass)
                                        defaultResource?.let {

                                            Timber.v(

                                                "$mTag Setting defaults from the " +
                                                        "resource: $defaultResource"
                                            )

                                            manager.setDefaults(it)
                                        }
                                    }

                                    manager.initialize(lifecycleCallback, persistence = persistence)

                                } else {

                                    failure.set(true)

                                    Timber.w("$mTag Not ready to initialize")

                                    latch.countDown()
                                }

                            } else {

                                Timber.w("$mTag Not supported")

                                latch.countDown()
                            }
                        }
                    }
                }

                latch.await()

                callback.onInitialization(!failure.get())
            }

        } catch (e: RejectedExecutionException) {

            callback.onInitialization(false, e)
        }
    }
}