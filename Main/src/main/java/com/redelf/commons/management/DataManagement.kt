package com.redelf.commons.management

import android.content.Context
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.context.Contextual
import com.redelf.commons.data.Empty
import com.redelf.commons.data.type.Typed
import com.redelf.commons.destruction.reset.Resettable
import com.redelf.commons.enable.Enabling
import com.redelf.commons.enable.EnablingCallback
import com.redelf.commons.execution.ExecuteWithResult
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.interruption.Abort
import com.redelf.commons.lifecycle.exception.InitializingException
import com.redelf.commons.lifecycle.exception.NotInitializedException
import com.redelf.commons.locking.Lockable
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.suspendable.Obtain
import com.redelf.commons.persistance.DBStorage
import com.redelf.commons.persistance.EncryptedPersistence
import com.redelf.commons.session.Session
import com.redelf.commons.transaction.Transaction
import com.redelf.commons.transaction.TransactionOperation
import java.util.UUID
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

abstract class DataManagement<T> :

    Management,
    Obtain<T?>,
    Resettable,
    Lockable,
    Abort,
    Enabling,
    Contextual<BaseApplication>,
    ExecuteWithResult<DataManagement.DataTransaction<T>>

{

    companion object {

        lateinit var STORAGE: EncryptedPersistence

        /*
            TODO:
             - Refactor - Move away from the static context access
             - Every manager to have its own version which is going to be appended to storage key
             - Obtain method to catch class cast exception, and to recreate the data object on catch
             - Make sure that persistence is independent on package path and class name
        */

        /*
            TODO:
                - Introduce the Data Binding mechanism to simplify the use
        */

        val DEBUG = AtomicBoolean()
        val ENCRYPT = AtomicBoolean()

        @Throws(IllegalArgumentException::class, IllegalStateException::class)
        fun initialize(ctx: Context) {

            DBStorage.initialize(ctx = ctx)

            STORAGE = EncryptedPersistence(

                ctx = ctx,
                doLog = DEBUG.get(),
                storageTag = "dt_mgmt"
            )
        }
    }

    protected abstract val storageKey: String

    protected open val typed: Typed<T>? = null
    protected open val persist: Boolean = true
    protected open val instantiateDataObject: Boolean = false

    private var data: T? = null
    private val locked = AtomicBoolean()
    private var enabled = AtomicBoolean(true)
    private var session = Session(name = javaClass.simpleName)

    protected abstract fun getLogTag(): String

    /*
        TODO: Cluster data object into smaller chunks so serialization and deserialization is improved
    */
    protected open fun createDataObject(): T? = null

    open fun canLog() = DEBUG.get()

    override fun abort() = Unit

    override fun enable(callback: EnablingCallback?) {

        enabled.set(true)
        callback?.onChange(enabled.get(), enabled.get())
    }

    override fun disable(callback: EnablingCallback?) {

        enabled.set(false)
        callback?.onChange(!enabled.get(), !enabled.get())
    }

    override fun isEnabled(): Boolean {

        return enabled.get()
    }

    override fun execute(what: DataTransaction<T>): Boolean {

        if (!isEnabled()) {

            return true
        }

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

        if (!isEnabled()) {

            return
        }

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

        if (!isEnabled()) {
            
            return
        }

        Console.log("${getLogTag()} Lock")

        abort()

        locked.set(true)
    }

    override fun unlock() {

        if (!isEnabled()) {

            return
        }

        Console.log("${getLogTag()} :: Unlock")

        locked.set(false)
    }

    final override fun isLocked(): Boolean {

        return locked.get()
    }

    final override fun isUnlocked() = !isLocked()

    @Throws(InitializingException::class, NotInitializedException::class)
    override fun obtain(): T? {

        if (!isEnabled()) {

            return null
        }

        val clazz = typed?.getClazz()
        val tag = "${getLogTag()} OBTAIN :: T = '${clazz?.simpleName}' ::"

        if (canLog()) Console.log("$tag START")

        if (isLocked()) {

            Console.warning("$tag Locked")

            return null
        }

        if (data != null) {

            if (canLog()) Console.log("$tag END: OK")

            return data
        }

        val dataObjTag = "$tag Data object ::"

        if (canLog()) Console.log("$dataObjTag Has initial: ${data != null}")

        if (data == null && persist) {

            var empty: Boolean? = null
            val key = storageKey
            val pulled = STORAGE.pull<T?>(key)

            if (pulled is Empty) {

                empty = pulled.isEmpty()
            }

            empty?.let {

                if (canLog()) Console.log("$dataObjTag Pulled :: Empty = $it")
            }

            if (empty == null) {

                if (canLog()) Console.log("$dataObjTag Pulled :: Null = ${data == null}")
            }

            pulled?.let {

                overwriteData(pulled)
            }

            if (canLog()) Console.debug("$dataObjTag Obtained from storage: $data")
        }

        if (canLog()) Console.log("$dataObjTag Intermediate: ${data != null}")

        if (instantiateDataObject) {

            var current: T? = data

            if (current == null) {

                current = createDataObject()

                current?.let {

                    data = current

                    doPushData(current)
                }

                if (current == null) {

                    throw IllegalStateException("Data object creation failed")
                }
            }

            if (canLog()) Console.log("$dataObjTag Instantiated: ${data != null}")
        }

        if (canLog()) Console.log("$dataObjTag Final: $data")

        return data
    }

    fun takeStorageKey() = storageKey

    @Throws(IllegalStateException::class)
    fun takeStorage(): EncryptedPersistence? {

        if (!isEnabled()) {

            return null
        }

        if (isLocked()) {

            return null
        }

        return STORAGE
    }

    @Throws(IllegalStateException::class)
    open fun pushData(data: T) {

        if (!isEnabled()) {

            return
        }

        if (DEBUG.get()) Console.log("${getLogTag()} Push data :: START")

        doPushData(data)
    }

    @Throws(IllegalStateException::class)
    protected fun doPushData(data: T) {

        if (!isEnabled()) {
            
            return
        }

        if (isLocked()) {

            Console.warning("${getLogTag()} Push data :: Locked: SKIPPING")

            onDataPushed(success = false)

            return
        }

        try {

            exec(

                onRejected = { e -> onDataPushed(err = e) }

            ) {

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

        if (success == true) {

            if (DEBUG.get()) Console.log("${getLogTag()} Data pushed")

        } else {

            if (DEBUG.get()) Console.error("${getLogTag()} Data push failed", err)
        }
    }

    override fun reset(): Boolean {

        if (!isEnabled()) {
            
            return true
        }

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

        if (!isEnabled()) {

            return
        }

        overwriteData()
    }

    protected fun overwriteData(data: T? = null) {

        if (!isEnabled()) {

            return
        }

        this.data = data
    }

    private fun initCallbacksTag() = "${getLogTag()} Data management initialization"

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
}
