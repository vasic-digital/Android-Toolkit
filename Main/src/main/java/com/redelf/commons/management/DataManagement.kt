package com.redelf.commons.management

import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.lifecycle.Initialization
import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.EncryptedPersistence
import java.util.concurrent.atomic.AtomicBoolean

abstract class DataManagement :

    Management,
    Initialization<EncryptedPersistence>,
    Obtain<EncryptedPersistence>

{

    protected var storage: EncryptedPersistence? = null

    private val initializing = AtomicBoolean(false)
    private val initCallbacksTag = "Data management initialization"

    private val initCallbacks =
        Callbacks<LifecycleCallback<EncryptedPersistence>>(initCallbacksTag)

    override fun initialize(callback: LifecycleCallback<EncryptedPersistence>) {

        if (initializing.get()) {

            initCallbacks.register(callback)
            return
        }

        initializing.set(true)

        storage = EncryptedPersistence()

        onInitialized()
    }

    override fun isInitialized() = storage != null

    override fun isInitializing() = initializing.get()

    @Throws(IllegalStateException::class)
    override fun obtain(): EncryptedPersistence {

        storage?.let {

            return it

        } ?: throw IllegalStateException("Storage is not initialized")
    }

    private fun onInitialized() {

        initializing.set(false)

        val doOnAllAction = object :
            CallbackOperation<LifecycleCallback<EncryptedPersistence>> {

            override fun perform(callback: LifecycleCallback<EncryptedPersistence>) {

                storage?.let {

                    callback.onInitialization(true, it)
                }

                if (storage == null) {

                    callback.onInitialization(false)
                }

                initCallbacks.unregister(callback)
            }
        }

        initCallbacks.doOnAll(doOnAllAction, initCallbacksTag)
    }
}