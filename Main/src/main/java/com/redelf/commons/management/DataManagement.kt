package com.redelf.commons.management

import android.content.Context
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.context.Contextual
import com.redelf.commons.execution.ExecuteWithResult
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.interruption.Abort
import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.lifecycle.exception.InitializingException
import com.redelf.commons.lifecycle.exception.NotInitializedException
import com.redelf.commons.locking.Lockable
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.DBStorage
import com.redelf.commons.persistance.EncryptedPersistence
import com.redelf.commons.reset.Resettable
import com.redelf.commons.session.Session
import com.redelf.commons.transaction.Transaction
import com.redelf.commons.transaction.TransactionOperation
import com.redelf.commons.type.Typed
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

abstract class DataManagement<T> :

    Management,
    Obtain<T?>,
    Resettable,
    Lockable,
    Abort,
    Contextual<BaseApplication>,
    ExecuteWithResult<DataManagement.DataTransaction<T>> {

    companion object {

        lateinit var STORAGE: EncryptedPersistence

        /*
            TODO: Refactor - Move away from the static context access
        */
        val DEBUG = AtomicBoolean()
        val ENCRYPT = AtomicBoolean()
        val LOG_RAW_DATA = AtomicBoolean()
        val LOGGABLE_MANAGERS: CopyOnWriteArrayList<Class<*>> = CopyOnWriteArrayList()
        val LOGGABLE_STORAGE_KEYS: CopyOnWriteArrayList<String> = CopyOnWriteArrayList()

        fun initialize(ctx: Context) {

            DBStorage.initialize(ctx = ctx)

            STORAGE = EncryptedPersistence(

                ctx = ctx,
                doLog = DEBUG.get(),
                storageTag = "dt_mgmt",
                doEncrypt = ENCRYPT.get(),
                logRawData = LOG_RAW_DATA.get(),
                logStorageKeys = LOGGABLE_STORAGE_KEYS
            )
        }
    }

    class DataTransaction<T>(

        val name: String,
        private val parent: DataManagement<T>,
        private val operation: TransactionOperation? = null

    ) : Transaction {

        companion object {

            /*
                TODO: Refactor - Move away from the static context access
            */
            val DEBUG = AtomicBoolean()
        }

        private val tag = "Transaction ::"
        private var session: UUID? = parent.session.takeIdentifier()
        private val canLog = DataManagement.DEBUG.get() && DEBUG.get()

        init {

            if (canLog) Console.log(

                "$tag Session: $session :: INIT :: $name :: " +
                    "With operation = ${operation != null}"
            )

            if (operation == null) {

                start()
            }
        }

        override fun start(): Boolean {

            session = parent.session.takeIdentifier()

            if (canLog) Console.log("$tag Session: $session :: START :: $name")

            return true
        }

        override fun perform(): Boolean {

            if (canLog) Console.log("$tag Session: $session :: PERFORM :: $name")

            operation?.let {

                val result =  parent.session.execute(operation)

                if (result) {

                    if (canLog) Console.log("$tag Session: $session :: PERFORMED :: $name")

                } else {

                    Console.error("$tag Session: $session :: FAILED :: $name")
                }

                return result
            }

            if (canLog) Console.log("$tag Session: $session :: PERFORMED :: $name")

            return true
        }

        override fun end(): Boolean {

            if (canLog) Console.log("$tag Session: $session :: ENDING :: $name")

            if (session != parent.session.takeIdentifier()) {

                if (canLog) Console.warning("$tag Session: $session :: SKIPPED :: $name")

                return false
            }

            var result = false

            try {

                val data = parent.obtain()

                data?.let {

                    parent.pushData(it)

                    result = true

                    if (canLog) Console.log("$tag Session: $session :: ENDED :: $name")
                }

            } catch (e: IllegalStateException) {

                Console.error(e)
            }

            if (!result) {

                if (canLog) Console.log("$tag Session: $session :: ENDING :: Failed: $name")
            }

            return result
        }

        fun getSession() = parent.session.takeName()
    }

    protected abstract val storageKey: String

    protected open val typed: Typed<T>? = null
    protected open val persist: Boolean = true
    protected open val instantiateDataObject: Boolean = false

    private var data: T? = null
    private val locked = AtomicBoolean()
    private var session = Session(name = javaClass.simpleName)

    private val initCallbacks =
        Callbacks<LifecycleCallback<EncryptedPersistence>>(initCallbacksTag())

    protected abstract fun getLogTag(): String

    /*
        TODO: Cluster data object into smaller chunks so serialization and deserialization is improved
    */
    protected open fun createDataObject(): T? = null

    protected open fun postInitialize(ctx: Context) = Unit

    /*
        TODO: Create another version of the method that will use into the account:
            if (LOGGABLE_STORAGE_KEYS.contains(storageKey)) { ...
            Instead of having it repeatedly used all over the codebase.
    */
    open fun canLog() = LOGGABLE_MANAGERS.isEmpty() ||
            LOGGABLE_MANAGERS.contains(javaClass)

    override fun abort() = Unit

    override fun execute(what: DataTransaction<T>): Boolean {

        val transaction = what.name
        val session = what.getSession()

        Console.log("Session: $session :: Execute :: START: $transaction")

        val started = what.start()

        if (started) {

            Console.log("Session: $session :: Execute :: STARTED: $transaction")

            val success = what.perform()

            if (success) {

                Console.log(

                    "Session: $session :: Execute :: PERFORMED :: $transaction :: Success"
                )

            } else {

                Console.error(

                    "Session: $session :: Execute :: PERFORMED :: $transaction :: Failure"
                )
            }

            return success
        }

        return false
    }

    fun transaction(name: String, action: Obtain<Boolean>) {

        execute(

            DataTransaction(

                name = name,
                parent = this,

                operation = object : TransactionOperation {

                    override fun perform() = action.obtain()
                }
            )
        )
    }

    fun transaction(name: String): Transaction = DataTransaction(name, this)

    override fun lock() {

        Console.log("${getLogTag()} Lock")

        abort()

        locked.set(true)
    }

    override fun unlock() {

        Console.log("${getLogTag()} :: Unlock")

        locked.set(false)
    }

    final override fun isLocked(): Boolean {

        return locked.get()
    }

    final override fun isUnlocked() = !isLocked()

    @Throws(InitializingException::class, NotInitializedException::class)
    override fun obtain(): T? {

        val clazz = typed?.getClazz()
        val tag = "${getLogTag()} Obtain :: T = '${clazz?.simpleName}' ::"

        if (LOGGABLE_STORAGE_KEYS.contains(storageKey)) {

            if (canLog()) Console.log("$tag START")
        }

        if (isLocked()) {

            Console.warning("$tag Locked")

            return null
        }

        if (data != null) {

            if (LOGGABLE_STORAGE_KEYS.contains(storageKey)) {

                if (canLog()) Console.log("$tag END: OK")
            }

            return data
        }

        val dataObjTag = "$tag Data object ::"

        if (canLog()) Console.log("$dataObjTag Initial: ${data != null}")

        if (data == null && persist) {

            val pulled = STORAGE.pull<T?>(storageKey)

            pulled?.let {

                overwriteData(pulled)
            }

            if (LOGGABLE_STORAGE_KEYS.contains(storageKey)) {

                if (canLog()) Console.debug("$dataObjTag Obtained from storage: $data")

            } else {

                if (canLog()) Console.log("$dataObjTag Obtained from storage: ${data != null}")
            }
        }

        if (canLog()) Console.log("$dataObjTag Intermediate: ${data != null}")

        if (instantiateDataObject) {

            var current: T? = data

            if (current == null) {

                current = createDataObject()

                current?.let {

                    doPushData(current)
                }

                if (current == null) {

                    throw IllegalStateException("Data object creation failed")
                }
            }

            if (canLog()) Console.log("$dataObjTag Instantiated: ${data != null}")
        }

        if (LOGGABLE_STORAGE_KEYS.contains(storageKey)) {

            if (canLog()) Console.log("$dataObjTag Final: $data")

        } else {

            if (canLog()) Console.log("$dataObjTag Final: ${data != null}")
        }

        return data
    }

    fun takeStorageKey() = storageKey

    @Throws(IllegalStateException::class)
    fun takeStorage(): EncryptedPersistence? {

        if (isLocked()) {

            return null
        }

        return STORAGE
    }

    @Throws(IllegalStateException::class)
    open fun pushData(data: T) {

        Console.log("${getLogTag()} Push data :: START")

        doPushData(data)
    }

    @Throws(IllegalStateException::class)
    protected fun doPushData(data: T) {

        if (isLocked()) {

            Console.warning("${getLogTag()} Push data :: Locked: SKIPPING")

            onDataPushed(success = false)

            return
        }

        try {

            exec {

                overwriteData(data)

                if (persist) {

                    try {

                        val store = takeStorage()
                        val pushed = store?.push(storageKey, data)

                        onDataPushed(success = pushed)

                    } catch (e: RejectedExecutionException) {

                        onDataPushed(err = e)
                    }

                } else {

                    onDataPushed(success = true)
                }
            }

        } catch (e: RejectedExecutionException) {

            onDataPushed(err = e)
        }
    }

    protected open fun onDataPushed(success: Boolean? = false, err: Throwable? = null) {

        if (canLog() && LOGGABLE_STORAGE_KEYS.contains(storageKey)) {

            if (success == true) {

                Console.log("${getLogTag()} Data pushed")

            } else {

                Console.error("${getLogTag()} Data push failed", err)
            }
        }
    }

    override fun reset(): Boolean {

        val tag = "${getLogTag()} :: Reset ::"

        Console.log("$tag START")

        try {

            session.reset()

            if (isNotEmpty(storageKey)) {

                Console.log("$tag Storage key: $storageKey")

                val s = takeStorage()

                if (instantiateDataObject) {

                    overwriteData(createDataObject())

                    data?.let {

                        doPushData(it)
                    }

                } else {

                    s?.delete(storageKey)
                }

            } else {

                Console.warning("$tag Empty storage key")
            }

            eraseData()

            Console.log("$tag END")

            return true

        } catch (e: RejectedExecutionException) {

            Console.error(tag, e)

        } catch (e: IllegalStateException) {

            Console.error(tag, e)
        }

        Console.error("$tag END: FAILED (2)")

        return false
    }

    override fun getWho(): String? = this::class.simpleName

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

    protected fun eraseData() {

        overwriteData()
    }

    protected fun overwriteData(data: T? = null) {

        this.data = data
    }

    private fun initCallbacksTag() = "${getLogTag()} Data management initialization"
}
