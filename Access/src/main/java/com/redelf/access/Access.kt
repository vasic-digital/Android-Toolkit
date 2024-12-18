package com.redelf.access

import com.redelf.access.implementation.AccessActivity
import com.redelf.access.implementation.FaceRecognitionAccess
import com.redelf.access.implementation.FingerprintAccess
import com.redelf.access.implementation.GenericAccess
import com.redelf.access.implementation.IrisAccess
import com.redelf.access.implementation.pin.PinAccess
import com.redelf.access.implementation.pin.PinAccessActivity
import com.redelf.access.installation.Installation
import com.redelf.access.installation.InstallationCheckCallback
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.capability.CapabilityCheckCallback
import com.redelf.commons.execution.Cancellation
import com.redelf.commons.execution.CommonExecution
import com.redelf.commons.execution.CommonExecutionCallback
import com.redelf.commons.execution.Executor
import com.redelf.commons.lifecycle.InitializationAsync
import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.logging.Console
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class Access(builder: AccessBuilder) :

    CommonExecution,
    Cancellation,
    InitializationAsync<Unit>,
    Installation {

    companion object {

        fun getGeneric(ctx: PinAccessActivity): Access {

            var priority = -1
            val generic = GenericAccess(++priority, ctx)

            return AccessBuilder()
                .addAccessMethod(generic)
                .build()
        }

        fun getDefault(ctx: AccessActivity): Access {

            var priority = -1
            val iris = IrisAccess(++priority, ctx)
            val faceAccess = FaceRecognitionAccess(++priority, ctx)
            val fingerprintAccess = FingerprintAccess(++priority, ctx)
            val pinAccess = PinAccess(++priority, ctx)

            return AccessBuilder()
                .addAccessMethod(pinAccess)
                .addAccessMethod(fingerprintAccess)
                .addAccessMethod(faceAccess)
                .addAccessMethod(iris)
                .build()
        }
    }

    private val executor = Executor.MAIN
    private val methods = builder.methods
    private val initializing = AtomicBoolean()
    private var method = AtomicReference<AccessMethod>()
    private val callbacks = Callbacks<LifecycleCallback<Unit>>(identifier = "Access lifecycle")

    private val initCallback = object : LifecycleCallback<Unit> {

        override fun onInitialization(success: Boolean, vararg args: Unit) {

            initializing.set(false)
            callbacks.doOnAll(object : CallbackOperation<LifecycleCallback<Unit>> {

                override fun perform(callback: LifecycleCallback<Unit>) {

                    callback.onInitialization(success)
                    callbacks.unregister(callback)
                }
            }, operationName = "Initialisation operation")
        }

        override fun onShutdown(success: Boolean, vararg args: Unit) {

            // Ignore.
        }
    }

    @Throws(IllegalStateException::class)
    override fun execute(callback: CommonExecutionCallback) {

        if (!isInitialized()) {

            throw  IllegalStateException("Not initialized")
        }
        method.get()?.execute(callback)
    }

    override fun cancel() {

        method.get()?.cancel()
    }

    override fun isInitialized() = method.get() != null

    override fun isInitializing() = isInitialized()

    
    fun isExecuting(): Boolean {

        method.get()?.let {

            return it.isExecuting()
        }
        return false
    }

    @Throws(IllegalStateException::class)
    override fun initialize(callback: LifecycleCallback<Unit>) {

        if (isInitialized()) {

            throw  IllegalStateException("Already initialized")
        }
        callbacks.register(callback)
        if (initializing.get()) {

            Console.warning("Already initializing")
            return
        }
        initializing.set(true)
        val action = Runnable {

            val iterator = methods.iterator()

            fun initialize(iterator: Iterator<AccessMethod>) {

                if (iterator.hasNext()) {

                    val accessMethod = iterator.next()

                    val capabilityCallback = object : CapabilityCheckCallback {

                        override fun onCapabilityChecked(capable: Boolean) {

                            if (capable) {

                                method.set(accessMethod)
                                initCallback.onInitialization(true)
                                Console.info("Access method: $method")
                            } else {

                                initialize(iterator)
                            }
                        }
                    }

                    accessMethod.checkCapability(capabilityCallback)

                } else {

                    Console.warning("No access method obtained")
                    initCallback.onInitialization(false)
                }
            }

            initialize(iterator)
        }

        executor.execute(action)
    }

    @Throws(IllegalStateException::class)
    override fun install() {

        if (!isInitialized()) {

            initialize(

                object : LifecycleCallback<Unit> {

                    override fun onInitialization(success: Boolean, vararg args: Unit) {

                        if (success) {

                            method.get()?.install()
                        } else {

                            throw IllegalStateException("Failed to initialize")
                        }
                    }

                    override fun onShutdown(success: Boolean, vararg args: Unit) {

                        throw IllegalStateException("Unexpected shutdown")
                    }
                }
            )
            return
        }
        method.get()?.install()
    }

    @Throws(IllegalStateException::class)
    override fun checkInstalled(callback: InstallationCheckCallback) {

        Console.log("Check installed")

        if (!isInitialized()) {

            initialize(

                object : LifecycleCallback<Unit> {

                    override fun onInitialization(success: Boolean, vararg args: Unit) {

                        if (success) {

                            method.get()?.checkInstalled(callback)

                        } else {

                            throw IllegalStateException("Failed to initialize")
                        }
                    }

                    override fun onShutdown(success: Boolean, vararg args: Unit) {

                        throw IllegalStateException("Unexpected shutdown")
                    }

                }
            )
            return
        }
        method.get()?.checkInstalled(callback)
    }

    override fun toString(): String {

        return "Access(methods=$methods)"
    }

    override fun initializationCompleted(e: Exception?) {

        Console.log("Initialization completed")
    }
}