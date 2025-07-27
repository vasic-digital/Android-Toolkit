package com.redelf.commons.data.wrapper.list

import com.redelf.commons.data.access.DataAccess
import com.redelf.commons.data.model.identifiable.Identifiable
import com.redelf.commons.destruction.delete.DeletionCheck
import com.redelf.commons.extensions.addAt
import com.redelf.commons.extensions.getAtIndex
import com.redelf.commons.extensions.isOnMainThread
import com.redelf.commons.extensions.onUiThread
import com.redelf.commons.extensions.recordException
import com.redelf.commons.extensions.removeAt
import com.redelf.commons.extensions.sync
import com.redelf.commons.extensions.yieldWhile
import com.redelf.commons.filtering.FilterAsync
import com.redelf.commons.filtering.FilterResult
import com.redelf.commons.lifecycle.initialization.InitializedCheck
import com.redelf.commons.lifecycle.termination.TerminationSynchronized
import com.redelf.commons.logging.Console
import com.redelf.commons.management.DataManagement
import com.redelf.commons.management.DataPushResult
import com.redelf.commons.modification.OnChangeCompleted
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.state.BusyCheck
import java.util.LinkedList
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

open class ListWrapper<T, M : DataManagement<*>>(

    val identifier: String,
    val environment: String = "default",

    private val dataAccess: DataAccess<T, M>? = null,
    private val onUi: Boolean,
    private var onChange: OnChangeCompleted? = null,
    private val onDataPushed: OnObtain<DataPushResult?>? = null

) : BusyCheck, InitializedCheck, TerminationSynchronized {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    private val busy = AtomicBoolean()
    private val list: CopyOnWriteArraySet<T> = CopyOnWriteArraySet()
    private val initialized = AtomicBoolean(dataAccess == null)
    private val executor: ExecutorService = Executors.newFixedThreadPool(1)

    private val dataPushListener: OnObtain<DataPushResult?>? = if (dataAccess != null) {

        object : OnObtain<DataPushResult?> {

            override fun onCompleted(data: DataPushResult?) {

                if (data?.success == true) {

                    onDataPushed("dataPushListener", data)

                } else {

                    onDataPushed?.onCompleted(data)

                    if (DEBUG.get()) {

                        Console.error(

                            "$tag dataPushListener :: " +
                                    "Changes :: None detected :: Data push failed"
                        )
                    }
                }
            }

            override fun onFailure(error: Throwable) {

                Console.error(error)
            }
        }

    } else {

        null
    }

    private val linkedManagersDataPushListener: OnObtain<DataPushResult?>? =
        if (dataAccess != null) {

            object : OnObtain<DataPushResult?> {

                override fun onCompleted(data: DataPushResult?) {

                    if (data?.success == true) {

                        onDataPushed("linkedManagersDataPushListener", data)

                    } else {

                        onDataPushed?.onCompleted(data)

                        if (DEBUG.get()) {

                            Console.error(

                                "$tag linkedManagersDataPushListener :: " +
                                        "Changes :: None detected :: Data push failed"
                            )
                        }
                    }
                }

                override fun onFailure(error: Throwable) {

                    Console.error(error)
                }
            }

        } else {

            null
        }

    init {

        exec {

            dataPushListener?.let {

                getManager()?.registerDataPushListener(it)
            }

            registerLinkedManagersDataPushListener()

            val action = "init"
            val from = action
            val items = getCollection(from)

            if (items == null || items.isEmpty()) {

                initialized.set(true)

                return@exec
            }

            replaceAllAndFilter(items, from) { modified, count ->

                initialized.set(true)

                if (modified) {

                    if (DEBUG.get()) {

                        Console.log(

                            "$tag Init :: " +
                                    "Changes :: Detected :: Count=$count" +
                                    ", getCollection().count=${items.size}"
                        )
                    }

                    notifyChanged(action = action)

                } else {

                    if (DEBUG.get()) {

                        Console.log(

                            "$tag Init :: " +
                                    "Changes :: None detected :: Count=$count" +
                                    ", getCollection().count=${items.size}"
                        )
                    }
                }
            }
        }
    }

    fun getManager(): M? {

        try {

            return dataAccess?.managerAccess?.obtain()

        } catch (e: Throwable) {

            recordException(e)
        }

        return null
    }

    fun getLinkedManagers(): List<Obtain<DataManagement<*>>>? {

        try {

            return dataAccess?.linkedManagers?.obtain()

        } catch (e: Throwable) {

            recordException(e)
        }

        return null
    }

    override fun terminate(vararg args: Any): Boolean {

        var success = true

        dataPushListener?.let {

            try {

                dataAccess?.managerAccess?.obtain()?.unregisterDataPushListener(dataPushListener)

            } catch (e: Throwable) {

                recordException(e)

                success = false
            }
        }

        linkedManagersDataPushListener?.let {

            val linkedManagers = getLinkedManagers()

            linkedManagers?.forEach { manager ->

                try {

                    manager.obtain().unregisterDataPushListener(it)

                } catch (e: Throwable) {

                    recordException(e)

                    success = false
                }
            }
        }

        return success
    }

    private val tag = "$identifier :: $environment :: ${getHashCode()} ::"

    override fun isBusy() = busy.get()

    override fun isInitialized() = initialized.get()

    override fun isNotInitialized() = !isInitialized()

    fun isEmpty(): Boolean {

        return isEmpty("isEmpty")
    }

    protected fun registerLinkedManagersDataPushListener() {

        linkedManagersDataPushListener?.let {

            val linkedManagers = getLinkedManagers()

            linkedManagers?.forEach { manager ->

                try {

                    manager.obtain().registerDataPushListener(it)

                } catch (e: Throwable) {

                    recordException(e)
                }
            }
        }
    }

    private fun isEmpty(from: String): Boolean {

        val empty = list.isEmpty()

        if (DEBUG.get()) {

            Console.log(

                "$tag Is empty :: From='$from', List=${list.hashCode()}, " +
                        "Size=${list.size}, Empty=$empty"
            )
        }

        return empty
    }

    fun isNotEmpty() = !isEmpty("isNotEmpty")

    fun get(index: Int): T? {

        return list.getAtIndex(index)
    }

    fun getLast(): T? {

        return list.last()
    }

    fun getFirst(): T? {

        return list.first()
    }

    fun indexOf(what: T): Int {

        return list.indexOf(what)
    }

    fun getHashCode(): Int {

        return list.hashCode()
    }

    fun getSize(): Int {

        return list.size
    }

    fun getList() = list.toList()

    fun contains(what: T) = list.contains(what)

    fun add(

        from: String,
        value: T,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (DEBUG.get()) Console.log("$tag add(value=$value), from='$from'")

        exec {

            doAdd(value, onChange, callback)
        }
    }

    fun remove(

        from: String,
        index: Int,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        Console.warning("$tag remove(index=$index), from='$from'")

        exec {

            doRemove(index, onChange, callback)
        }
    }

    fun remove(

        from: String,
        what: T,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        Console.warning("$tag remove(what='$what'), from='$from'")

        exec {

            doRemove(what, onChange, callback)
        }
    }

    fun update(

        from: String,
        what: T,
        where: Int,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        Console.log("$tag update(what='$what, where=$where), from='$from'")

        exec {

            doUpdate(what, where, onChange, callback)
        }
    }

    fun removeAll(

        from: String,
        what: Collection<T>,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        Console.warning("$tag removeAll(index=$what), from='$from'")

        exec {

            doRemoveAll(what, onChange, callback)
        }
    }

    fun clear(

        from: String,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        Console.warning("$tag clear(), from='$from'")

        exec {

            doClear("clear(from='$from')", onChange, false, callback)
        }
    }

    fun replaceAllAndFilter(

        what: Collection<T?>?,
        from: String,
        removeDeleted: Boolean = true,
        filters: List<FilterAsync<T>> = emptyList(),
        onChange: OnChangeCompleted? = null,
        callback: ((modified: Boolean, changedCount: Int) -> Unit)? = null

    ) {

        val from = "replaceAllAndFilter(from='$from',size=${what?.size ?: 0})"

        exec {

            doAddAllAndFilter(what, from, true, removeDeleted, filters, onChange, callback)
        }
    }

    fun addAllAndFilter(

        what: Collection<T?>?,
        from: String,
        replace: Boolean = false,
        removeDeleted: Boolean = true,
        filters: List<FilterAsync<T>> = emptyList(),
        onChange: OnChangeCompleted? = null,
        callback: ((modified: Boolean, changedCount: Int) -> Unit)? = null

    ) {

        exec {

            doAddAllAndFilter(what, from, replace, removeDeleted, filters, onChange, callback)
        }
    }

    fun addAll(

        what: Collection<T>,
        from: String,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (DEBUG.get()) Console.log("$tag addAll(), from='$from'")

        exec {

            doAddAll(what, onChange, callback)
        }
    }

    fun purge(

        from: String,
        onChange: OnChangeCompleted? = null,
        callback: ((purgedCount: Int) -> Unit)? = null

    ) {

        val tag = "$tag purge(from='$from')"

        Console.log("$tag START")

        exec {

            doPurge(onChange, callback)
        }
    }

    private fun notifyChanged(

        skipNotifying: Boolean = false,
        onChange: OnChangeCompleted? = null,
        action: String

    ) {

        if (onUi) {

            onUiThread {

                onChange?.onChange(action, true)

                if (!skipNotifying) {

                    this@ListWrapper.onChange?.onChange(action, true)
                }
            }

        } else {

            onChange?.onChange(action, true)

            if (!skipNotifying) {

                this@ListWrapper.onChange?.onChange(action, true)
            }
        }
    }

    private fun notifyCallback(callback: (() -> Unit)?) {

        callback?.let {

            if (onUi) {

                onUiThread {

                    it()
                }

            } else {

                it()
            }
        }
    }

    private fun doAdd(

        value: T,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (list.add(value)) {

            notifyChanged(false, onChange, "add")
        }

        notifyCallback(callback)
    }

    private fun doAddAll(

        what: Collection<T>,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (list.addAll(what)) {

            notifyChanged(false, onChange, "addAll.${what.size}")
        }

        notifyCallback(callback)
    }

    private fun doUpdate(

        what: T,
        where: Int,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        try {

            if (list.removeAt(where) != null) {

                list.addAt(where, what)

                if (list.elementAt(where) == what) {

                    notifyChanged(false, onChange, "update.$where")
                }
            }


        } catch (e: Throwable) {

            recordException(e)
        }

        notifyCallback(callback)
    }

    private fun doClear(

        from: String,
        onChange: OnChangeCompleted? = null,
        skipNotifying: Boolean = false,
        callback: (() -> Unit)? = null

    ) {

        list.clear()

        if (list.isEmpty()) {

            notifyChanged(

                skipNotifying = skipNotifying,
                onChange,
                "clear(from='$from')",
            )
        }

        notifyCallback(callback)
    }

    private fun doAddAllAndFilter(

        what: Collection<T?>?,
        from: String,
        replace: Boolean = false,
        removeDeleted: Boolean = true,
        filters: List<FilterAsync<T>> = emptyList(),
        onChange: OnChangeCompleted? = null,
        callback: ((modified: Boolean, changedCount: Int) -> Unit)? = null

    ) {

        if (DEBUG.get()) Console.log("doAddAllAndFilter(from='$from')")

        fun next() {

            exec {

                var modified = false
                var changedCount = 0
                val linked = LinkedList(what ?: emptyList())

                val toRemove = mutableListOf<T>()
                val toUpdate = mutableListOf<T>()

                list.forEach { wItem ->

                    var where = -1
                    var found: T? = null

                    linked.forEach { lItem ->

                        if (found == null) {

                            if (lItem is Number && wItem is Number) {

                                if (lItem == wItem) {

                                    found = lItem

                                    if (lItem != wItem) {

                                        where = indexOf(wItem)
                                    }
                                }

                            } else if (lItem is Identifiable<*> && wItem is Identifiable<*>) {

                                if (lItem.getId() == wItem.getId()) {

                                    found = lItem

                                    if (lItem != wItem) {

                                        where = indexOf(wItem)
                                    }
                                }

                            } else {

                                val e = IllegalArgumentException("Non-identifiable items found")
                                Console.warning(e)
                            }
                        }
                    }

                    if (found != null) {

                        toUpdate.add(found)

                        if (where > -1) {

                            doUpdate(found, where)
                        }

                    } else {

                        toRemove.add(wItem)
                    }
                }

                if (toRemove.isNotEmpty()) {

                    modified = true

                    changedCount += toRemove.size

                    doRemoveAll(toRemove)
                }

                if (toUpdate.isNotEmpty()) {

                    changedCount += toUpdate.size
                }

                linked.forEach { linked ->

                    linked?.let { lkd ->

                        if (!toUpdate.contains(lkd) && !toRemove.contains(lkd)) {

                            modified = true

                            changedCount++

                            doAdd(lkd)
                        }
                    }
                }

                fun filter() {

                    fun notify() {

                        if (modified) {

                            notifyChanged(false, onChange, "addAllAndFilter.${what?.size ?: 0}")
                        }

                        callback?.let {

                            if (onUi) {

                                onUiThread {

                                    it(modified, changedCount)
                                }

                            } else {

                                it(modified, changedCount)
                            }
                        }
                    }

                    if (filters.isEmpty()) {

                        notify()

                    } else {

                        val filtered = FilterResult(filteredItems = list, wasModified = false)

                        filters.forEach { filter ->

                            val res = sync(

                                debug = true,
                                context = "${identifier}.filter.each"

                            ) { callback ->

                                filter.filter(filtered.filteredItems, callback)
                            }

                            filtered.filteredItems.clear()
                            filtered.filteredItems.addAll(res?.filteredItems ?: emptyList())

                            filtered.changedCount.set(

                                filtered.changedCount.get() + (res?.changedCount?.get() ?: 0)
                            )

                            if (!filtered.modified.get()) {

                                filtered.modified.set(res?.isModified() == true)
                            }
                        }

                        if (!modified) {

                            modified = filtered.modified.get()
                        }

                        changedCount += filtered.changedCount.get()

                        if (filtered != list) {

                            doClear(

                                "doAddAllAndFilter(from='$from," +
                                        "filteredCount=${filtered.filteredItems.size}'," +
                                        "originalCount=${list.size})",

                                skipNotifying = true

                            ) {

                                doAddAll(filtered.filteredItems) {

                                    notify()
                                }
                            }

                        } else {

                            notify()
                        }
                    }
                }

                if (removeDeleted) {

                    doPurge { purgedCount ->

                        if (purgedCount > 0) {

                            modified = true
                            changedCount += purgedCount
                        }

                        filter()
                    }

                } else {

                    filter()
                }
            }
        }

        if (replace) {

            doClear(

                "doAddAllAndFilter(from='$from," +
                        "filteredCount=NONE'," +
                        "originalCount=${list.size})",

                skipNotifying = true

            ) {

                next()
            }

        } else {

            next()
        }
    }

    private fun doPurge(

        onChange: OnChangeCompleted? = null,
        callback: ((purgedCount: Int) -> Unit)? = null

    ) {

        val toRemove = mutableListOf<T>()

        list.forEach { message ->

            if (message is DeletionCheck && message.isDeleted()) {

                toRemove.add(message)
            }
        }

        if (toRemove.isEmpty()) {

            if (DEBUG.get()) Console.log("$tag END :: Nothing to remove")

        } else {

            Console.debug("$tag Removing ${toRemove.size} items")

            doRemoveAll(

                toRemove,

                onChange = object : OnChangeCompleted {

                    override fun onChange(action: String, changed: Boolean) {

                        if (changed) {

                            notifyChanged(false, onChange, "purge")
                        }
                    }
                }

            ) {

                if (DEBUG.get()) Console.log("$tag END")
            }
        }

        callback?.let {

            it(toRemove.size)
        }
    }

    private fun doRemoveAll(

        what: Collection<T>,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (list.removeAll(what)) {

            notifyChanged(false, onChange, "removeAll.${what.size}")
        }

        notifyCallback(callback)
    }

    private fun doRemove(

        what: T,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (list.remove(what)) {

            notifyChanged(false, onChange, "remove")
        }

        notifyCallback(callback)
    }

    private fun doRemove(

        index: Int,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        list.removeAt(index)?.let {

            notifyChanged(false, onChange, "remove.$index")
        }

        notifyCallback(callback)
    }

    @Synchronized
    private fun exec(what: () -> Unit) {

        busy.set(true)

        try {

            executor.execute {

                try {

                    what()

                } catch (e: Throwable) {

                    Console.error(

                        "$tag Execute :: ERROR: ${e.message ?: e::class.simpleName}"
                    )

                    recordException(e)
                }

                busy.set(false)
            }

        } catch (e: Throwable) {

            busy.set(false)

            Console.error("$tag Pre-Execute :: ERROR: ${e.message}")

            recordException(e)
        }
    }

    protected open fun getCollection(from: String): Collection<T?>? {

        if (isOnMainThread()) {

            val e = IllegalStateException("Shall not obtain collection from the main thread")
            recordException(e)
        }

        try {

            val manager = getManager()

            if (manager == null) {

                Console.error(

                    "$tag getCollection(from='$from') :: Manager is null"
                )

                return null
            }

            yieldWhile {

                manager.isBusy() || manager.isReading() || manager.isWriting()
            }

            val coll = dataAccess?.obtain()

            if (DEBUG.get()) {

                Console.log(
                    "$tag Get collection :: From='$from', " +
                            "getCollection().count=${coll?.size ?: 0}"
                )
            }

            return coll

        } catch (e: Throwable) {

            Console.log(

                "$tag Fet collection :: " +
                        "From='$from', getCollection().count=0, " +
                        "Error=${e.message ?: e::class.simpleName}"
            )

            recordException(e)
        }

        return null
    }

    private fun onDataPushed(pushContext: String, data: DataPushResult) {

        val items = getCollection("$pushContext(pushFrom='${data.pushFrom}')")

        val from =
            "$pushContext(pushFrom='${data.pushFrom}',size=${items?.size ?: 0})"

        onDataPushed?.onCompleted(data)

        replaceAllAndFilter(

            from = from,
            what = items

        ) { modified, count ->

            if (modified) {

                if (DEBUG.get()) {

                    Console.log(

                        "$tag $pushContext :: " +
                                "Changes :: Detected :: Count=$count" +
                                ", getCollection().count=${items?.size ?: 0}"
                    )
                }

                notifyChanged(action = "$pushContext.dataPushed")

            } else {

                if (DEBUG.get()) {

                    Console.log(

                        "$tag $pushContext :: " +
                                "Changes :: None detected :: Count=$count" +
                                ", getCollection().count=${items?.size ?: 0}"
                    )
                }
            }
        }
    }
}