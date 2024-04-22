package com.redelf.commons.management.managers

import android.content.Context
import com.redelf.commons.context.Contextual
import com.redelf.commons.defaults.ResourceDefaults
import com.redelf.commons.exec
import com.redelf.commons.instantiation.SingleInstance
import com.redelf.commons.management.DataManagement
import com.redelf.commons.management.Management
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.persistance.EncryptedPersistence
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

class ManagersReInitializer {

    fun reInitializeManagerInstances(

        context: Context? = null,
        managers: MutableList<DataManagement<*>>,
        defaultResources: Map<Class<*>, Int>? = null

    ) : Boolean {

        if (managers.isEmpty()) {

            return true
        }

        val cleaner = ManagersCleaner()
        val cleanupResult = cleaner.cleanupManagers(managers)

        if (!cleanupResult) {

            return false
        }

        val success = AtomicBoolean()
        val latch = CountDownLatch(1)

        val callback = object : OnObtain<Boolean> {

            override fun onCompleted(data: Boolean) {

                success.set(data)

                latch.countDown()
            }

            override fun onFailure(error: Throwable) {

                Timber.e(error)

                success.set(false)

                latch.countDown()
            }
        }

        reInitializeManagerInstances(

            context = context,
            callback = callback,
            managers = managers,
            defaultResources = defaultResources
        )

        latch.await()

        return success.get()
    }

    fun reInitializeManagers(

        context: Context? = null,
        managers: MutableList<SingleInstance<*>>,
        defaultResources: Map<Class<*>, Int>? = null

    ) : Boolean {

        val success = AtomicBoolean()
        val latch = CountDownLatch(1)

        val callback = object : OnObtain<Boolean> {

            override fun onCompleted(data: Boolean) {

                success.set(data)

                latch.countDown()
            }

            override fun onFailure(error: Throwable) {

                Timber.e(error)

                success.set(false)

                latch.countDown()
            }
        }

        reInitializeManagers(

            context = context,
            callback = callback,
            managers = managers,
            defaultResources = defaultResources
        )

        latch.await()

        return success.get()
    }

    private fun reInitializeManagerInstances(

        context: Context? = null,
        callback: OnObtain<Boolean>,
        managers: MutableList<DataManagement<*>>,
        defaultResources: Map<Class<*>, Int>? = null

    ) {

        try {

            exec {

                val ok = resetManagerInstances(managers)

                if (!ok) {

                    callback.onCompleted(false)
                    return@exec
                }

                val failure = AtomicBoolean()

                managers.forEach { manager ->

                    if (manager is Contextual) {

                        context?.let { ctx ->

                            Timber.v(

                                "Manager: ${manager.getWho()} " +
                                        "injecting context: $ctx"
                            )

                            manager.injectContext(ctx)
                        }
                    }

                    if (manager is ResourceDefaults) {

                        val defaultResource = defaultResources?.get(manager.javaClass)

                        defaultResource?.let {

                            Timber.v(

                                "Manager: ${manager.getWho()} " +
                                        "setting defaults from resource: $defaultResource"
                            )

                            manager.setDefaults(it)
                        }
                    }
                }

                callback.onCompleted(!failure.get())
            }

        } catch (e: RejectedExecutionException) {

            callback.onFailure(e)
        }
    }

    private fun reInitializeManagers(

        context: Context? = null,
        callback: OnObtain<Boolean>,
        managers: MutableList<SingleInstance<*>>,
        defaultResources: Map<Class<*>, Int>? = null

    ) {

        val tag = "Reinitialize managers ::"

        Timber.v("$tag START")

        try {

            exec {

                Timber.v("$tag We are going to reset managers")

                val ok = resetManagers(managers)

                if (!ok) {

                    Timber.e("$tag Managers failed to reset with success")

                    callback.onCompleted(false)
                    return@exec
                }

                Timber.v("$tag Managers have been reset with success")

                val failure = AtomicBoolean()

                managers.forEach { m ->

                    exec {

                        val manager = m.obtain()

                        if (manager is DataManagement<*>) {

                            val mTag = "$tag ${manager.getWho()} :: ${manager.hashCode()}"

                            Timber.v("$mTag START")

                            if (manager is Contextual) {

                                context?.let { ctx ->

                                    Timber.v(

                                        "Manager: ${manager.getWho()} " +
                                                "injecting context: $ctx"
                                    )

                                    manager.injectContext(ctx)
                                }
                            }

                            if (manager is ResourceDefaults) {

                                val defaultResource = defaultResources?.get(manager.javaClass)

                                defaultResource?.let {

                                    Timber.v(

                                        "Manager: ${manager.getWho()} " +
                                                "setting defaults from resource: $defaultResource"
                                    )

                                    manager.setDefaults(it)
                                }
                            }

                            Timber.v("$mTag Ready to initialize")

                        } else {

                            failure.set(true)
                        }
                    }
                }

                callback.onCompleted(!failure.get())
            }

        } catch (e: RejectedExecutionException) {

            callback.onFailure(e)
        }
    }

    private fun resetManagers(managers: MutableList<SingleInstance<*>>): Boolean {

        val result = AtomicBoolean(true)
        val latch = CountDownLatch(managers.size)

        managers.forEach { m ->

            exec {

                if (!m.reset()) {

                    result.set(false)
                }

                latch.countDown()
            }
        }

        latch.await()

        return result.get()
    }

    private fun resetManagerInstances(managers: MutableList<DataManagement<*>>): Boolean {

        val result = AtomicBoolean()
        val cleaner = ManagersCleaner()
        val latch = CountDownLatch(1)

        val callback = object : ManagersCleaner.CleanupCallback {

            override fun onCleanup(success: Boolean, error: Throwable?) {

                result.set(success)

                latch.countDown()
            }

            override fun onCleanup(manager: Management, success: Boolean, error: Throwable?) {

                error?.let {

                    Timber.e(error)
                }
            }
        }

        cleaner.cleanupManagers(

            managers = managers,
            callback = callback
        )

        latch.await()

        return result.get()
    }
}