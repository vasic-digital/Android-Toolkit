package com.redelf.commons.management

import com.redelf.commons.BuildConfig
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.execution.TaskExecutor
import com.redelf.commons.isEmpty
import com.redelf.commons.isNotEmpty
import com.redelf.commons.lifecycle.Initialization
import com.redelf.commons.lifecycle.InitializationPerformer
import com.redelf.commons.lifecycle.InitializationReady
import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.lifecycle.exception.InitializingException
import com.redelf.commons.lifecycle.exception.NotInitializedException
import com.redelf.commons.locking.Lockable
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
    InitializationReady,
    Obtain<T?>,
    Resettable,
    Lockable {

    protected abstract val storageKey: String
    protected open val instantiateDataObject: Boolean = false
    protected open val storageClassificationIdentifierRequired = false
    protected open val storageExecutor = TaskExecutor.instantiateSingle()

    private var data: T? = null
    private val locked = AtomicBoolean()
    private var classificationIdentifier: String? = null
    private var encStorage: EncryptedPersistence? = null
    private val initializing = AtomicBoolean(false)

    private val initCallbacks =
        Callbacks<LifecycleCallback<EncryptedPersistence>>(initCallbacksTag())

    protected abstract fun getInitTag(): String

    protected open fun getStorageClassificationIdentifier(): String? = null

    override fun canInitialize() = isUnlocked()

    override fun initializationReady(): Boolean {

        if (storageClassificationIdentifierRequired) {

            val identifier = getStorageClassificationIdentifier()
            return isNotEmpty(identifier)
        }

        return true
    }

    fun initialize(

        callback: LifecycleCallback<EncryptedPersistence>,
        persistence: EncryptedPersistence? = null,

        ) {

        if (isLocked()) {

            callback.onInitialization(true)
            return
        }

        persistence?.let {

            encStorage = it
        }

        initialize(callback)
    }

    final override fun initialize(callback: LifecycleCallback<EncryptedPersistence>) {

        val sKey = getFullStorageKey()

        if (sKey == null) {

            Timber.w("No storage key available for ${getWho()}")

            callback.onInitialization(false)
            return
        }

        initCallbacks.register(callback)

        if (initializing.get()) {

            return
        }

        try {

            storageExecutor.execute {

                initializing.set(true)

                val store = createStorage()

                if (store == null) {

                    val e = NotInitializedException(who = getWho())
                    onInitializationFailed(e)

                } else {

                    data = store.pull(sKey)

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

    override fun isUnlocked() = !isLocked()

    @Throws(IllegalStateException::class)
    override fun initialization(): Boolean {

        Timber.v("DataManagement :: initialization")
        return true
    }

    override fun isInitialized() = isLocked() || (encStorage != null && !isInitializing())

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
    fun takeStorage(): EncryptedPersistence? {

        if (isLocked()) {

            return null
        }

        encStorage?.let {

            return it

        } ?: throw NotInitializedException("Storage")
    }

    @Throws(IllegalStateException::class)
    open fun pushData(data: T) {

        if (isLocked()) {

            Timber.w("DataManagement :: pushData :: Locked: SKIPPING")

            return
        }

        val sKey = getFullStorageKey()
            ?: throw IllegalStateException("No storage key available for ${getWho()}")

        val store = takeStorage()

        this.data = data

        try {

            storageExecutor.execute {

                store?.push(sKey, data)
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

        val sKey = getFullStorageKey()
            ?: throw IllegalStateException("No storage key available for ${getWho()}")

        val store = takeStorage()

        this.data = data

        return store?.push(sKey, data) ?: false
    }

    override fun reset(): Boolean {

        val tag = "DataManagement :: Reset :: ${getWho()}"

        Timber.v("$tag START")

        try {

            var result = true
            val sKey = getFullStorageKey()

            sKey?.let {

                if (isNotEmpty(sKey)) {

                    Timber.v("$tag Storage key: $sKey")

                    result = this.encStorage?.delete(sKey) ?: true

                } else {

                    Timber.w("$tag Empty storage key")
                }
            }

            if (sKey == null) {

                Timber.w("$tag No storage key available")
            }

            this.data = null
            this.encStorage = null

            if (result) {

                Timber.v("$tag END")

            } else {

                Timber.e("$tag END: FAILED (1)")
            }

            return result

        } catch (e: RejectedExecutionException) {

            Timber.e(tag, e)

        } catch (e: IllegalStateException) {

            Timber.e(tag, e)
        }

        Timber.e("$tag END: FAILED (2)")

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

    override fun getWho(): String? = this::class.simpleName

    override fun onInitializationFailed(e: Exception) {

        val doOnAllAction = object :
            CallbackOperation<LifecycleCallback<EncryptedPersistence>> {

            override fun perform(callback: LifecycleCallback<EncryptedPersistence>) {

                Timber.e(e)

                if (isLocked()) {

                    callback.onInitialization(false)

                } else {

                    encStorage?.let {

                        callback.onInitialization(true, it)
                    }

                    if (encStorage == null) {

                        callback.onInitialization(false)
                    }
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

                if (isLocked()) {

                    callback.onInitialization(false)

                } else {

                    encStorage?.let {

                        callback.onInitialization(true, it)
                    }

                    if (encStorage == null) {

                        callback.onInitialization(false)
                    }
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

    private fun getFullStorageKey(): String? {

        if (storageClassificationIdentifierRequired) {

            if (isEmpty(classificationIdentifier)) {

                classificationIdentifier = getStorageClassificationIdentifier()
            }

            if (isEmpty(classificationIdentifier)) {

                return null
            }

        } else {

            classificationIdentifier = "0"
        }

        if (BuildConfig.DEBUG) {

            return "${storageKey}.${classificationIdentifier}.DEBUG" +
                    ".${BaseApplication.getVersionCode()}"
        }

        return "${storageKey}.${classificationIdentifier}"
    }

    private fun createStorage(): EncryptedPersistence? {

        if (isLocked()) {

            return null
        }

        encStorage?.let {

            return it
        }

        val key = getFullStorageKey()

        key?.let {

            val store = EncryptedPersistence(storageTag = it)

            encStorage = store

            return store
        }

        return null
    }

    private fun initCallbacksTag() = "${getInitTag()} Data management initialization"
}