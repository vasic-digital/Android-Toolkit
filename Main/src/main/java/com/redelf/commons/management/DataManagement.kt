package com.redelf.commons.management

import android.content.Context
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.context.Contextual
import com.redelf.commons.data.Empty
import com.redelf.commons.data.type.Typed
import com.redelf.commons.destruction.reset.Resettable
import com.redelf.commons.destruction.reset.ResettableAsync
import com.redelf.commons.enable.Enabling
import com.redelf.commons.enable.EnablingCallback
import com.redelf.commons.environment.Environment
import com.redelf.commons.execution.CommonExecutionCallback
import com.redelf.commons.execution.ExecuteWithResult
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.isOnMainThread
import com.redelf.commons.extensions.recordException
import com.redelf.commons.extensions.sync
import com.redelf.commons.interruption.Abort
import com.redelf.commons.lifecycle.exception.InitializingException
import com.redelf.commons.lifecycle.exception.NotInitializedException
import com.redelf.commons.locking.Lockable
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.obtain.ObtainAsync
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.persistance.EncryptedPersistence
import com.redelf.commons.persistance.database.DBStorage
import com.redelf.commons.registration.Registration
import com.redelf.commons.session.Session
import com.redelf.commons.state.BusyCheck
import com.redelf.commons.state.ReadingCheck
import com.redelf.commons.state.WritingCheck
import com.redelf.commons.transaction.Transaction
import com.redelf.commons.transaction.TransactionOperation
import com.redelf.commons.versioning.Versionable
import java.util.UUID
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

abstract class DataManagement<T> :

    Abort,
    Lockable,
    Enabling,
    BusyCheck,
    Management,
    Resettable,
    Environment,
    ReadingCheck,
    WritingCheck,
    ObtainAsync<T?>,
    ResettableAsync,
    Contextual<BaseApplication>,
    Registration<OnObtain<Boolean?>>,
    ExecuteWithResult<DataManagement.DataTransaction<T>> where T : Versionable {

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
    private val reading = AtomicBoolean()
    private val writing = AtomicBoolean()
    private var enabled = AtomicBoolean(true)
    private val lastDataVersion = AtomicLong(-1)
    private var session = Session(name = javaClass.simpleName)
    private val pushCallbacks = Callbacks<OnObtain<Boolean?>>("on_push")

    protected abstract fun getLogTag(): String

    protected open fun createDataObject(): T? = null

    open fun canLog() = DEBUG.get()

    override fun abort() = Unit

    override fun getEnvironment() = Environment.DEFAULT

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

    override fun isReading(): Boolean {

        return reading.get()
    }

    override fun isWriting(): Boolean {

        return writing.get()
    }

    override fun isBusy(): Boolean {

        return isReading() || isWriting()
    }

    override fun register(subscriber: OnObtain<Boolean?>) {

        if (pushCallbacks.isRegistered(subscriber)) {

            return
        }

        pushCallbacks.register(subscriber)
    }

    override fun unregister(subscriber: OnObtain<Boolean?>) {

        if (pushCallbacks.isRegistered(subscriber)) {

            pushCallbacks.unregister(subscriber)
        }
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

    fun getData(): T? = obtain()

    fun obtain(): T? {

        return sync("${getWho()}.obtain") { callback ->

            obtain(

                callback
            )
        }
    }

    @Throws(InitializingException::class, NotInitializedException::class)
    override fun obtain(callback: OnObtain<T?>) {

        exec(

            onRejected = { e ->

                callback.onFailure(e)
            }

        ) {

            if (!isEnabled()) {

                callback.onCompleted(null)
                return@exec
            }

            val clazz = typed?.getClazz()
            val tag = "${getLogTag()} OBTAIN :: T = '${clazz?.simpleName}' ::"

            if (canLog()) Console.log("$tag START")

            if (isLocked()) {

                Console.warning("$tag Locked")

                callback.onCompleted(null)
                return@exec
            }

            if (data != null) {

                if (canLog()) Console.log("$tag END: OK")

                callback.onCompleted(data)

                return@exec
            }

            val dataObjTag = "$tag Data object ::"

            if (canLog()) Console.log("$dataObjTag Has initial: ${data != null}")

            var empty: Boolean? = null
            val key = storageKey

            fun onPulled(pulled: T?) {

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

                if (canLog()) Console.log("$dataObjTag Intermediate: ${data != null}")

                if (instantiateDataObject) {

                    var current: T? = data

                    if (current == null) {

                        current = createDataObject()

                        current?.let {

                            data = current
                        }

                        if (current == null) {

                            throw IllegalStateException("Data object creation failed")
                        }
                    }

                    if (canLog()) Console.log("$dataObjTag Instantiated: ${data != null}")
                }

                if (canLog()) Console.log("$dataObjTag Final: $data")

                reading.set(false)

                callback.onCompleted(data)
            }

            if (data == null && persist) {

                reading.set(true)

                STORAGE.pull(

                    key,

                    object : OnObtain<T?> {

                        override fun onCompleted(data: T?) {

                            onPulled(data)
                        }

                        override fun onFailure(error: Throwable) {

                            callback.onFailure(error)
                        }
                    }
                )

            } else {

                onPulled(data)
            }
        }
    }

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

    fun pushData(): Boolean {

        val data = obtain()

        return pushData(data)
    }

    fun pushData(data: T?): Boolean {

        if (isOnMainThread()) {

            val msg = "Push data is not recommended to perform on the main thread"
            val e = IllegalStateException(msg)
            recordException(e)
        }

        return sync("${getWho()}.pushData") { callback ->

            pushData(data, callback)

        } == true
    }

    open fun pushData(data: T?, callback: OnObtain<Boolean?>?) {

        if (!isEnabled()) {

            callback?.onCompleted(false)
            return
        }

        if (DEBUG.get()) Console.log("${getLogTag()} Push data")

        data?.let {

            doPushData(it, retry = 0, callback)
        }

        if (data == null) {

            val dObject = createDataObject()

            dObject?.let {

                doPushData(it, retry = 0, callback)
            }

            if (dObject == null) {

                val msg = "Data object creation failed, can't push data"

                Console.error("${getLogTag()} Push data :: $msg")

                callback?.onFailure(IllegalStateException(msg))
            }
        }
    }

    protected fun doPushData(data: T, retry: Int = 0, callback: OnObtain<Boolean?>? = null) {

        val callbackWrapper = object : OnObtain<Boolean?> {

            override fun onCompleted(data: Boolean?) {

                notifyOnPushCompleted(data == true)

                callback?.onCompleted(data)
            }

            override fun onFailure(error: Throwable) {

                callback?.onFailure(error)
            }
        }

        if (!isEnabled()) {

            onDataPushed(success = false)
            callbackWrapper.onCompleted(data = false)
            return
        }

        if (isLocked()) {

            Console.warning("${getLogTag()} Push data :: Locked: SKIPPING")

            onDataPushed(success = false)
            callbackWrapper.onCompleted(data = false)
            return
        }

        exec {

            if (isBusy()) {

                if (retry <= 5) {

                    Console.warning(

                        "${getLogTag()} BUSY :: Rescheduling data push :: Retry = $retry"
                    )

                    exec(

                        delayInMilliseconds = 10 * 1000

                    ) {

                        doPushData(data, retry = retry + 1, callback)
                    }

                } else {

                    val e = IllegalArgumentException("Data push failed, manager is busy")
                    recordException(e)
                }

                return@exec
            }

            if (overwriteData(data) && persist) {

                writing.set(true)

                try {

                    val version = data.getVersion()

                    /*
                        FIXME: Polish this condition.
                    */
                    if (version <= 0 || version > lastDataVersion.get()) {

                        val store = takeStorage()

                        val pushed = store?.push(

                            key = storageKey, what = data,

                            check = object : Obtain<Boolean> {

                                override fun obtain(): Boolean {

                                    return !isReading()
                                }
                            }
                        )

                        if (store == null) {

                            Console.error("${getLogTag()} Push data :: No store available")
                        }

                        val success = pushed == true

                        if (success) {

                            lastDataVersion.set(version)

                            /*
                                TODO/FIXME:
                                 - This has to be left to end user, not manager
                                 - Add shutdown hook (cleanup service) so saving is
                                 performed and no data loss happens
                            */
                            data.increaseVersion()

                            Console.log(

                                "${getLogTag()} Data pushed :: " +
                                        "Version = ${lastDataVersion.get()}, " +
                                        "New version = ${data.getVersion()}"
                            )
                        }

                        onDataPushed(success = success)
                        callbackWrapper.onCompleted(data = success)

                    } else {

                        val msg = "Can't push data version $version, " +
                                "the last pushed data version is ${lastDataVersion.get()}"

                        val e = java.lang.IllegalArgumentException(msg)

                        onDataPushed(err = e)
                        callbackWrapper.onFailure(e)
                    }

                } catch (e: RejectedExecutionException) {

                    onDataPushed(err = e)
                    callbackWrapper.onFailure(e)
                }

            } else {

                writing.set(false)

                onDataPushed(success = true)
                callbackWrapper.onCompleted(data = true)
            }
        }
    }

    protected open fun onDataPushed(success: Boolean? = false, err: Throwable? = null) {

        writing.set(false)

        if (success == true) {

            if (DEBUG.get()) Console.log("${getLogTag()} Data pushed")

        } else {

            if (DEBUG.get()) {

                val msg = err?.message ?: "Unknown"

                Console.error("${getLogTag()} Data push failed, Error = '$msg'")
            }
        }
    }

    override fun reset(): Boolean {

        if (isOnMainThread()) {

            val msg = "Reset is not recommended to perform on the main thread"
            val e = IllegalStateException(msg)
            recordException(e)
        }

        return sync("${getWho()}.reset") { callback ->

            reset(callback)

        } == true
    }

    override fun reset(callback: OnObtain<Boolean?>) {

        val tag = "${getLogTag()} Reset ::"

        if (!isEnabled()) {

            Console.warning("$tag DISABLED")
            callback.onCompleted(false)
            return
        }

        Console.log("$tag START")

        exec(

            onRejected = { e ->

                Console.error("$tag FAILED :: Error='${e.message}'")
                callback.onFailure(e)
            }

        ) {

            if (DEBUG.get()) Console.log("$tag STARTED")

            try {

                if (DEBUG.get()) Console.log("$tag To reset session")

                session.reset()

                if (DEBUG.get()) Console.log("$tag Session reset")

                if (isNotEmpty(storageKey)) {

                    if (DEBUG.get()) Console.log("$tag Storage key = '$storageKey'")

                    fun completeReset(success: Boolean?) {

                        if (success == true) {

                            if (DEBUG.get()) Console.log("$tag Completing reset")

                            eraseData()

                            Console.log("$tag END")

                            callback.onCompleted(true)

                        } else {

                            Console.error("$tag Complete reset failed")

                            callback.onCompleted(success)
                        }
                    }

                    val s = takeStorage()

                    if (instantiateDataObject) {

                        createDataObject()?.let { dObject ->

                            overwriteData(dObject)

                            data?.let {

                                doPushData(

                                    data = it,

                                    retry = 0,

                                    callback = object : OnObtain<Boolean?> {

                                        override fun onCompleted(data: Boolean?) {

                                            completeReset(data)
                                        }

                                        override fun onFailure(error: Throwable) {

                                            callback.onFailure(error)
                                        }
                                    }
                                )
                            }

                            if (data == null) {

                                Console.error("$tag Data object creation failed")
                                completeReset(false)
                                return@exec
                            }
                        }

                    } else {

                        s?.delete(

                            storageKey,

                            object : OnObtain<Boolean?> {

                                override fun onCompleted(data: Boolean?) {

                                    completeReset(true)
                                }

                                override fun onFailure(error: Throwable) {

                                    callback.onFailure(error)
                                }
                            })
                    }

                } else {

                    Console.warning("$tag END :: Empty storage key")

                    callback.onCompleted(false)
                }

            } catch (e: Throwable) {

                Console.error("$tag FAILED :: Error='${e.message}'")

                callback.onFailure(e)
            }
        }
    }

    override fun getWho(): String? = this::class.simpleName

    protected fun eraseData() {

        if (!isEnabled()) {

            return
        }

        Console.log("${getLogTag()} Data :: Erase :: START")

        this.data = null

        Console.log("${getLogTag()} Data :: Erase :: END")
    }

    protected fun overwriteData(data: T): Boolean {

        if (!isEnabled()) {

            return false
        }

        // FIXME: Polish and add environment into the account
        //  when it is changed (to reset version to 0)
        if (data.getVersion() >= (this.data?.getVersion() ?: 0)) {

            Console.log(
                "${getLogTag()} Data :: Overwrite :: " +
                        "From version = ${this.data?.getVersion() ?: 0}, " +
                        "To version = ${data.getVersion()}"
            )

            if (this.data != data) {

                this.data = data

                return true

            } else {

                Console.warning("${getLogTag()} Data :: Overwrite :: SKIPPED (1)")
            }

        } else {

            Console.warning("${getLogTag()} Data :: Overwrite :: SKIPPED (2)")
        }

        return false
    }

    private fun notifyOnPushCompleted(success: Boolean) {

        pushCallbacks.doOnAll(object : CallbackOperation<OnObtain<Boolean?>> {

            override fun perform(callback: OnObtain<Boolean?>) {

                callback.onCompleted(success)
            }

        }, operationName = "push.completed")
    }

    class DataTransaction<T>(

        val name: String,
        private val parent: DataManagement<T>,
        private val operation: TransactionOperation? = null

    ) : Transaction where T : Versionable {

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

                val result = parent.session.execute(operation)

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

            return sync("Transaction.end.$name") { callback ->

                end(callback)

            } ?: false
        }

        override fun end(callback: OnObtain<Boolean?>) {

            exec {

                if (canLog) Console.log("$tag Session: $session :: ENDING :: $name")

                if (session != parent.session.takeIdentifier()) {

                    if (canLog) Console.warning("$tag Session: $session :: SKIPPED :: $name")

                    callback.onCompleted(false)
                    return@exec
                }

                try {

                    parent.obtain(

                        object : OnObtain<T?> {

                            override fun onCompleted(data: T?) {

                                data?.let {

                                    parent.pushData(

                                        it,

                                        object : OnObtain<Boolean?> {

                                            override fun onCompleted(data: Boolean?) {

                                                val result = data == true

                                                if (canLog) Console.log("$tag Session: $session :: ENDED :: $name")

                                                callback.onCompleted(result)
                                            }

                                            override fun onFailure(error: Throwable) {

                                                callback.onFailure(error)
                                            }
                                        }
                                    )
                                }

                                if (data == null) {

                                    callback.onCompleted(false)
                                }
                            }

                            override fun onFailure(error: Throwable) {

                                callback.onFailure(error)
                            }
                        }
                    )

                } catch (e: Throwable) {

                    callback.onFailure(e)
                }
            }
        }

        fun getSession() = parent.session.takeName()
    }
}
