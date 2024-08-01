package com.redelf.commons.transmission

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.contain.Contain
import com.redelf.commons.context.Contextual
import com.redelf.commons.data.Empty
import com.redelf.commons.destruction.clear.Clearing
import com.redelf.commons.destruction.delete.Removal
import com.redelf.commons.execution.Executor
import com.redelf.commons.execution.TaskExecutor
import com.redelf.commons.extensions.recordException
import com.redelf.commons.iteration.Iterable
import com.redelf.commons.logging.Console
import com.redelf.commons.management.DataManagement
import com.redelf.commons.management.Management
import com.redelf.commons.measure.Size
import com.redelf.commons.modification.Add
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.security.encryption.Encryption
import java.security.GeneralSecurityException
import java.util.*
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

abstract class TransmissionManager<T, D>(private val dataManager: Obtain<DataManagement<T>>) :

    Add<D>,
    Management,
    Contextual<BaseApplication> where T : Empty, T : Clearing,
                                      T : Size, T : Add<D>, T : Iterable<D>,
                                      T : Contain<D>, T : Removal<D> {

    companion object {

        const val BROADCAST_ACTION_SEND = "TransmissionManager.Action.SEND"
        const val BROADCAST_EXTRA_RESULT = "TransmissionManager.Extra.RESULT"
        const val BROADCAST_ACTION_RESULT = "TransmissionManager.Action.RESULT"
    }

    private val capacity = 100
    private val maxRetries = 10
    private var managedData: T? = null
    private val sending = AtomicBoolean()
    private var lastSendingTime: Long = 0
    private val sequentialExecutor = TaskExecutor.instantiateSingle()

    private val sendingCallbacks =
        Callbacks<TransmissionSendingCallback<D>>(identifier = "Transmission sending")

    private val persistCallbacks =
        Callbacks<TransmissionManagerPersistCallback>(identifier = "Transmission persistence")

    protected open val minSendIntervalInSeconds = 0

    protected abstract val sendingDefaultStrategy: TransmissionManagerSendingStrategy<D>
    protected abstract var currentSendingStrategy: TransmissionManagerSendingStrategy<D>

    private val sendRequestReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            Console.log("BROADCAST_ACTION_SEND on receive")

            intent?.let {

                if (it.action == BROADCAST_ACTION_SEND) {

                    Console.log("BROADCAST_ACTION_SEND on action")

                    try {

                        send(executedFrom = "BROADCAST_ACTION_SEND")

                    } catch (e: IllegalStateException) {

                        Console.error(e)
                    }
                }
            }
        }
    }

    fun setSendingStrategy(sendingStrategy: TransmissionManagerSendingStrategy<D>): Boolean {

        currentSendingStrategy = sendingStrategy

        return currentSendingStrategy == sendingStrategy
    }

    fun getSendingStrategy() = currentSendingStrategy

    init {

        initialize()
    }

    @Throws(IllegalStateException::class)
    private fun initialize() {

        val intentFilter = IntentFilter(BROADCAST_ACTION_SEND)
        registerReceiver(sendRequestReceiver, intentFilter)

        Console.log("BROADCAST_ACTION_SEND receiver registered")

        val action = Runnable {

            try {

                val dManager = dataManager.obtain()
                managedData = dManager.obtain()

            } catch (e: Exception) {

                recordException(e)

                onInit(false)

                return@Runnable
            }

            onInit(true)
        }

        try {

            Executor.MAIN.execute(action)

        } catch (e: RejectedExecutionException) {

            recordException(e)
        }
    }

    private fun shutdown() {

        val terminated = terminate()
        onShutdown(terminated)
    }

    @Throws(IllegalStateException::class)
    fun send(data: D, async: Boolean = true) {

        val action = Runnable {

            Console.log("We are about to send data")

            persist(data)
        }

        if (async) {

            try {

                sequentialExecutor.execute(action)

            } catch (e: RejectedExecutionException) {

                recordException(e)
            }

        } else {

            action.run()
        }
    }

    /*
        Note: Returns True if there was something to update
     */
    @Throws(IllegalStateException::class)
    fun update(data: D): Boolean {

        return reSchedule(data)
    }

    private fun unSchedule(scheduled: T): T? {

        doUnSchedule(scheduled)?.let {

            persist()

            return it
        }

        return null
    }

    private fun reSchedule(scheduled: D): Boolean {

        if (doReSchedule(scheduled)) {

            persist()

            return true
        }

        return false
    }

    @Throws(IllegalStateException::class)
    fun send(executedFrom: String = "") {

        Console.log("Send (manager) :: executedFrom='$executedFrom'")

        val action = Runnable { executeSending("send") }
        sequentialExecutor.execute(action)
    }

    @Throws(IllegalStateException::class)
    fun deleteAll() {

        if (isSending()) {

            throw IllegalStateException("Data are being sent")
        }

        val action = Runnable {

            Console.warning("We are about to delete all data")
            clear()
            persist()
        }

        sequentialExecutor.execute(action)
    }

    @Throws(IllegalStateException::class)
    fun delete(item: D, callback: OnObtain<Boolean>) {

        if (isSending()) {

            throw IllegalStateException("Data are being sent")
        }

        val action = Runnable {

            Console.warning("We are about to delete the data item")

            if (clear(item)) {

                Console.log("The data item has been deleted with success")

                persist()

                callback.onCompleted(true)

            } else {

                Console.log("The data item has failed to delete")

                callback.onCompleted(false)
            }
        }

        sequentialExecutor.execute(action)
    }

    fun getScheduledCount(): Long {

        return managedData?.getSize() ?: 0
    }

    abstract fun getScheduled(): Collection<T>

    abstract fun doUnSchedule(scheduled: T): T?

    abstract fun doReSchedule(scheduled: D): Boolean

    fun isSending() = sending.get()

    fun addSendingCallback(callback: TransmissionSendingCallback<D>) {

        sendingCallbacks.register(callback)
    }

    fun removeSendingCallback(callback: TransmissionSendingCallback<D>) {

        sendingCallbacks.unregister(callback)
    }

    fun addPersistCallback(callback: TransmissionManagerPersistCallback) {

        persistCallbacks.register(callback)
    }

    fun removePersistCallback(callback: TransmissionManagerPersistCallback) {

        persistCallbacks.unregister(callback)
    }

    fun resetSendingStrategy(): Boolean {

        currentSendingStrategy = sendingDefaultStrategy
        return currentSendingStrategy == sendingDefaultStrategy
    }

    override fun add(data: D): Boolean {

        Console.log("Data: Add")

        return managedData?.add(data) == true
    }

    protected abstract fun decrypt(encrypted: String, encryption: Encryption<String, String>): T

    private fun executeSending(executedFrom: String = "") {

        val now = System.currentTimeMillis()

        val timeDiff = now - lastSendingTime

        val timeCondition = minSendIntervalInSeconds > 0 &&
                timeDiff < minSendIntervalInSeconds * 1000

        if (timeCondition) {

            return
        }

        Console.log("Last sending executed before: %s", timeDiff)

        if (currentSendingStrategy.isNotReady()) {

            Console.warning("Current sending strategy is not ready")

            return
        }

        if (isSending()) {

            Console.warning("Data is already sending")

            return
        }

        Console.log("Execute sending :: executedFrom='$executedFrom'")

        setSending(true)

        var persistingRequired = false

        if (managedData?.isEmpty() == true) {

            Console.warning("No data to be sent yet")
            setSending(false)

            return
        }

        val iterator = managedData?.getIterator()

        while (iterator?.hasNext() == true) {

            val item = iterator.next()

            item?.let { data ->

                Console.log("Data decrypted")

                onSendingStarted(data)

                val success = executeSending(data)

                if (success) {

                    lastSendingTime = System.currentTimeMillis()

                    iterator.remove()

                    if (!persistingRequired) {

                        persistingRequired = true
                    }

                    Console.info("Data has been sent")

                } else {

                    Console.error("Data has not been sent")
                }

                onSent(data, success)
            }
        }

        setSending(false)

        if (persistingRequired) {

            persist()
        }
    }

    private fun persist(data: D) {

        try {

            if (managedData?.contains(data) == true) {

                Console.warning("Data has been already persisted: %s", data)

                executeSending("persist")

            } else {

                add(data)

                persist()

                Console.log("Data has been persisted: %s", data)
            }

        } catch (e: OutOfMemoryError) {

            recordException(e)

        } catch (e: Exception) {

            recordException(e)
        }
    }

    private fun executeSending(data: D): Boolean {

        if (currentSendingStrategy == sendingDefaultStrategy) {

            val default = "DEFAULT SENDING STRATEGY"
            Console.log("Executing sending of %s with '%s'", data, default)

        } else {

            val custom = "CUSTOM SENDING STRATEGY"
            Console.debug("Executing sending of %s with '%s'", data, custom)
        }

        return currentSendingStrategy.executeSending(data)
    }

    private fun persist() {

        var success = false

        try {

            managedData?.let { data ->

                dataManager.obtain().pushData(data)

                success = true
            }

        } catch (e: Exception) {

            recordException(e)
        }

        if (success) {

            Console.info("Data has been persisted")

        } else {

            Console.error("Data has not been persisted")
        }

        onPersisted(success)
    }

    private fun onInit(success: Boolean) {

        if (success) {

            Console.log("Init success")
            return
        }

        Console.error("Init failed")
    }

    private fun onShutdown(success: Boolean) {

        if (success) {

            Console.log("Shutdown success")
            return
        }

        Console.error("Shutdown failed")
    }

    private fun onSent(data: D, success: Boolean) {

        val intent = Intent(BROADCAST_ACTION_RESULT)
        intent.putExtra(BROADCAST_EXTRA_RESULT, success)

        val ctx = takeContext()
        ctx.sendBroadcast(intent)

        Console.log("BROADCAST_ACTION_RESULT on sent")

        val operation = object : CallbackOperation<TransmissionSendingCallback<D>> {

            override fun perform(callback: TransmissionSendingCallback<D>) {

                callback.onSent(data, success)
            }
        }

        sendingCallbacks.doOnAll(operation, "onSent")
    }

    private fun onSendingStarted(data: D) {

        val operation = object : CallbackOperation<TransmissionSendingCallback<D>> {

            override fun perform(callback: TransmissionSendingCallback<D>) {

                callback.onSendingStarted(data)
            }
        }

        sendingCallbacks.doOnAll(operation, "onSendingStarted")
    }

    private fun onPersisted(success: Boolean) {

        Console.log("On data persisted: %b", success)

        val operation = object : CallbackOperation<TransmissionManagerPersistCallback> {

            override fun perform(callback: TransmissionManagerPersistCallback) {

                callback.onPersisted(success)
            }
        }

        persistCallbacks.doOnAll(operation, "On persisted")

        if (managedData?.isNotEmpty() == true) {

            if (success) {

                Console.log("On data persisted: We are about to start sending data")

                executeSending("onPersisted")


            } else {

                Console.error("On data NOT persisted: We are NOT going to start data sending")
            }
        }
    }

    private fun terminate(): Boolean {

        try {

            unregisterReceiver(sendRequestReceiver)

            Console.log("BROADCAST_ACTION_SEND receiver unregistered")

        } catch (e: IllegalArgumentException) {

            Console.error(e)
        }

        clear()

        return managedData?.isEmpty() == true
    }

    @Throws(InterruptedException::class)
    private fun add(items: LinkedList<D>) {

        Console.log("Data: Add, count: %d", items.size)

        items.forEach {

            managedData?.add(it)
        }
    }

    private fun clear() {

        Console.log("Data: Clear")

        managedData?.clear()
    }

    private fun clear(item: D): Boolean {

        Console.log("Data: Clear item")

        try {

            if (managedData?.contains(item) == true) {

                return managedData?.remove(item) == true
            }

        } catch (e: GeneralSecurityException) {

            recordException(e)

        } catch (e: OutOfMemoryError) {

            recordException(e)
        }

        return false
    }

    private fun setSending(sending: Boolean) {

        Console.log("Setting: Sending data to %b", sending)

        this.sending.set(sending)
    }

    protected fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent? {

        receiver?.let { r ->
            filter?.let { f ->

                LocalBroadcastManager.getInstance(takeContext()).registerReceiver(r, f)
            }
        }

        return null
    }

    protected fun unregisterReceiver(receiver: BroadcastReceiver?) {

        receiver?.let {

            LocalBroadcastManager.getInstance(takeContext()).unregisterReceiver(it)
        }
    }

    protected fun sendBroadcast(intent: Intent?) {

        intent?.let {

            LocalBroadcastManager.getInstance(takeContext()).sendBroadcast(it)
        }
    }

    override fun takeContext(): BaseApplication {

        return BaseApplication.takeContext()
    }
}