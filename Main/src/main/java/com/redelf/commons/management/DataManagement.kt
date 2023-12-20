package com.redelf.commons.management

import com.redelf.commons.Credentials
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.exec
import com.redelf.commons.lifecycle.Initialization
import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.lifecycle.exception.InitializingException
import com.redelf.commons.lifecycle.exception.NotInitializedException
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.EncryptedPersistence
import timber.log.Timber
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

abstract class DataManagement<T> :

    Management,
    Initialization<EncryptedPersistence>,
    Obtain<T?>

{

    protected abstract val storageKey: String

    private var data: T? = null
    private var storage: EncryptedPersistence? = null
    private val initializing = AtomicBoolean(false)
    private val initCallbacksTag = "Data management initialization"

    private val initCallbacks =
        Callbacks<LifecycleCallback<EncryptedPersistence>>(initCallbacksTag)


    fun initialize(

        callback: LifecycleCallback<EncryptedPersistence>,
        persistence: EncryptedPersistence? = null,

    ) {

        persistence?.let {

            storage = it
        }

        initialize(callback)
    }

    override fun initialize(callback: LifecycleCallback<EncryptedPersistence>) {

        initCallbacks.register(callback)

        if (initializing.get()) {

            return
        }

        try {

            exec {

                initializing.set(true)

                val store = createStorage()

                data = store.pull(storageKey)

                onInitialized()
            }

        } catch (e: RejectedExecutionException) {

            onInitialized(e)
        }
    }

    override fun isInitialized() = storage != null

    override fun isInitializing() = initializing.get()

    @Throws(InitializingException::class, NotInitializedException::class)
    override fun obtain(): T? {

        if (isInitializing()) {

            throw InitializingException()
        }

        if (!isInitialized()) {

            throw NotInitializedException()
        }

        return data
    }

    @Throws(IllegalStateException::class)
    fun takeStorage(): EncryptedPersistence {

        storage?.let {

            return it

        } ?: throw NotInitializedException("Storage")
    }

    @Throws(IllegalStateException::class)
    open fun pushData(data: T) {

        val store = takeStorage()

        this.data = data

        try {

            exec {

                store.push(storageKey, data)
            }

        } catch (e: RejectedExecutionException) {

            Timber.e(e)
        }
    }

    private fun onInitialized(e: Exception? = null) {

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

        initializing.set(false)
    }

    private fun createStorage(): EncryptedPersistence {

        storage?.let {

            return it
        }

        val store = EncryptedPersistence()
        storage = store
        return store
    }
}