package com.redelf.commons.management

import android.os.Build
import com.redelf.commons.BuildConfig
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.execution.Executor
import com.redelf.commons.execution.TaskExecutor
import com.redelf.commons.isNotEmpty
import com.redelf.commons.lifecycle.Initialization
import com.redelf.commons.lifecycle.InitializationPerformer
import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.lifecycle.exception.InitializingException
import com.redelf.commons.lifecycle.exception.NotInitializedException
import com.redelf.commons.locking.Lock
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.EncryptedPersistence
import com.redelf.commons.recordException
import com.redelf.commons.reset.Resettable
import timber.log.Timber
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

abstract class DataManagement<T> :

    Management,
    Initialization<EncryptedPersistence>,
    InitializationPerformer,
    Obtain<T?>,
    Resettable,
    Lock

{

    protected abstract val storageKey: String
    protected open val instantiateDataObject: Boolean = false
    protected open val storageExecutor = TaskExecutor.instantiateSingle()

    private var data: T? = null
    private val locked = AtomicBoolean()
    private var storage: EncryptedPersistence? = null
    private val initializing = AtomicBoolean(false)

    private val initCallbacks =
        Callbacks<LifecycleCallback<EncryptedPersistence>>(initCallbacksTag())

    protected abstract fun getInitTag(): String

    fun initialize(

        callback: LifecycleCallback<EncryptedPersistence>,
        persistence: EncryptedPersistence? = null,

        ) {

        persistence?.let {

            storage = it
        }

        initialize(callback)
    }

    final override fun initialize(callback: LifecycleCallback<EncryptedPersistence>) {

        initCallbacks.register(callback)

        if (initializing.get()) {

            return
        }

        try {

            storageExecutor.execute {

                initializing.set(true)

                val store = createStorage()

                data = store.pull(storageKey)

                try {

                    if (initialization()) {

                        onInitializationCompleted()

                    } else {

                        val e = NotInitializedException(who = getWho())
                        onInitializationFailed(e)
                    }

                } catch (e: IllegalStateException) {

                    onInitializationFailed(e)
                }
            }

        } catch (e: RejectedExecutionException) {

            onInitializationFailed(e)
        }
    }

    protected open fun createDataObject(): T? = null

    override fun lock() {

        Timber.v("DataManagement :: Lock")

        locked.set(true)
    }

    override fun unlock() {

        Timber.v("DataManagement :: Unlock")

        locked.set(false)
    }

    override fun isLocked(): Boolean {

        return locked.get()
    }

    @Throws(IllegalStateException::class)
    override fun initialization(): Boolean {

        Timber.v("DataManagement :: initialization")
        return true
    }

    override fun isInitialized() = storage != null && !isInitializing()

    override fun isInitializing() = initializing.get()

    @Throws(InitializingException::class, NotInitializedException::class)
    override fun obtain(): T? {

        if (isLocked()) {

            Timber.w("DataManagement :: Obtain :: Locked")
            return null
        }

        if (isInitializing() || isNotInitialized()) {

            Initialization.waitForInitialization(

                who = this,
                initLogTag = initCallbacksTag()
            )
        }

        if (instantiateDataObject) {

            var current: T? = data

            if (current == null) {

                current = createDataObject()

                current?.let {

                    pushData(current)
                }

                if (current == null) {

                    throw IllegalStateException("Data object creation failed")
                }
            }
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

        if (isLocked()) {

            Timber.w("DataManagement :: pushData :: Locked: SKIPPING")

            return
        }

        val store = takeStorage()

        this.data = data

        try {

            storageExecutor.execute {

                store.push(storageKey, data)
            }

        } catch (e: RejectedExecutionException) {

            Timber.e(e)
        }
    }

    @Throws(IllegalStateException::class)
    fun pushDataSynchronized(data: T): Boolean {

        if (isLocked()) {

            Timber.w("DataManagement :: pushData :: Locked: SKIPPING")

            return false
        }

        val store = takeStorage()

        this.data = data

        return store.push(storageKey, data)
    }

    override fun reset(): Boolean {

        try {

            val store = takeStorage()

            this.data = null

            return store.delete(storageKey)

        } catch (e: RejectedExecutionException) {

            recordException(e)

        } catch (e: IllegalStateException) {

            recordException(e)
        }

        return false
    }

    override fun initializationCompleted(e: Exception?) {

        if (e == null) {

            if (instantiateDataObject) {

                try {

                    var current: T? = obtain()

                    if (current == null) {

                        current = createDataObject()

                        current?.let {

                            pushData(current)
                        }

                        if (current == null) {

                            throw IllegalStateException("Data object creation failed")
                        }
                    }

                } catch (e: IllegalStateException) {

                    initializationCompleted(e)

                } catch (e: IllegalArgumentException) {

                    initializationCompleted(e)
                }
            }
        }

        if (e == null) {

            Timber.v("DataManagement :: initialization completed with success")

        } else {

            Timber.e(e, "DataManagement :: initialization completed with failure")
        }
    }

    protected open fun getWho(): String? = null

    override fun onInitializationFailed(e: Exception) {

        val doOnAllAction = object :
            CallbackOperation<LifecycleCallback<EncryptedPersistence>> {

            override fun perform(callback: LifecycleCallback<EncryptedPersistence>) {

                Timber.e(e)

                storage?.let {

                    callback.onInitialization(true, it)
                }

                if (storage == null) {

                    callback.onInitialization(false)
                }

                initCallbacks.unregister(callback)
            }
        }

        initializing.set(false)
        initCallbacks.doOnAll(doOnAllAction, initCallbacksTag())

        initializationCompleted(e)
    }

    override fun onInitializationCompleted() {

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

        initializing.set(false)
        initCallbacks.doOnAll(doOnAllAction, initCallbacksTag())

        initializationCompleted()
    }

    @Throws(IllegalStateException::class)
    protected fun getData(): T {

        val data = obtain()

        data?.let {

            return it
        }

        val who = getWho()
        val baseMsg = "data is null"

        val msg = if (isNotEmpty(who)) {

            "$who obtained $baseMsg"

        } else {

            "Obtained $baseMsg"
        }

        throw IllegalStateException(msg)
    }

    private fun createStorage(): EncryptedPersistence {

        storage?.let {

            return it
        }

        val sKey = if (BuildConfig.DEBUG) {

            "${storageKey}.DEBUG.${BaseApplication.getVersionCode()}"

        } else {

            storageKey
        }

        val store = EncryptedPersistence(storageTag = sKey)
        storage = store
        return store
    }

    private fun initCallbacksTag() = "${getInitTag()} Data management initialization"
}