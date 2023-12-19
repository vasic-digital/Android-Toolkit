package com.redelf.commons.management

import com.redelf.commons.Credentials
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.exec
import com.redelf.commons.lifecycle.Initialization
import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.persistance.EncryptedPersistence
import timber.log.Timber
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

abstract class DataManagement<T> :

    Management,
    Initialization<EncryptedPersistence>

{

    protected var data: T? = null
    protected var storage: EncryptedPersistence? = null

    protected abstract val storageKey: String

    private val initializing = AtomicBoolean(false)
    private val initCallbacksTag = "Data management initialization"

    private val initCallbacks =
        Callbacks<LifecycleCallback<EncryptedPersistence>>(initCallbacksTag)

    override fun initialize(callback: LifecycleCallback<EncryptedPersistence>) {

        if (initializing.get()) {

            initCallbacks.register(callback)
            return
        }

        try {

            exec {

                initializing.set(true)

                storage = EncryptedPersistence()

                data = storage?.pull(storageKey)

                onInitialized()
            }

        } catch (e: RejectedExecutionException) {

            onInitialized(e)
        }
    }

    override fun isInitialized() = storage != null

    override fun isInitializing() = initializing.get()

    @Throws(IllegalStateException::class)
    fun getStorage(): EncryptedPersistence {

        storage?.let {

            return it

        } ?: throw IllegalStateException("Storage is not initialized")
    }

    private fun onInitialized(e: Exception? = null) {

        initializing.set(false)

        val doOnAllAction = object :
            CallbackOperation<LifecycleCallback<EncryptedPersistence>> {

            override fun perform(callback: LifecycleCallback<EncryptedPersistence>) {

                e?.let {

                    Timber.e(e)
                }

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