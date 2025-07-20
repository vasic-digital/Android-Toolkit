package com.redelf.commons.data.wrapper.list

import com.redelf.commons.data.access.DataAccess
import com.redelf.commons.data.model.identifiable.Identifiable
import com.redelf.commons.destruction.delete.DeletionCheck
import com.redelf.commons.extensions.onUiThread
import com.redelf.commons.extensions.recordException
import com.redelf.commons.extensions.sync
import com.redelf.commons.filtering.Filter
import com.redelf.commons.filtering.FilterAsync
import com.redelf.commons.filtering.FilterResult
import com.redelf.commons.lifecycle.initialization.InitializedCheck
import com.redelf.commons.lifecycle.termination.TerminationSynchronized
import com.redelf.commons.logging.Console
import com.redelf.commons.management.DataManagement
import com.redelf.commons.modification.OnChangeCompleted
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.state.BusyCheck
import java.util.LinkedList
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

open class ListWrapper<T, M : DataManagement<*>>(

    val identifier: String,
    val environment: String = "default",

    private val dataAccess: DataAccess<T, M>? = null,
    private val onUi: Boolean,
    private var onChange: OnChangeCompleted? = null,
    private val onDataPushed: OnObtain<Boolean?>? = null

) : BusyCheck, InitializedCheck, TerminationSynchronized {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    private val busy = AtomicBoolean()
    private var list: CopyOnWriteArrayList<T> = CopyOnWriteArrayList()
    private val initialized = AtomicBoolean(dataAccess == null)
    private val executor: ExecutorService = Executors.newFixedThreadPool(1)

    private val dataPushListener: OnObtain<Boolean?>? = if (dataAccess != null) {

        object : OnObtain<Boolean?> {

            override fun onCompleted(data: Boolean?) {

                if (data == true) {

                    val items = getCollection()

                    replaceAllAndFilter(items, "dataPushListener") { modified, cont ->

                        onDataPushed?.onCompleted(data && modified)
                    }

                } else {

                    onDataPushed?.onCompleted(data)
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

            val items = getCollection()

            replaceAllAndFilter(items, "init") { _, _ ->

                initialized.set(true)
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

    override fun terminate(vararg args: Any): Boolean {

        dataPushListener?.let {

            try {

                dataAccess?.managerAccess?.obtain()?.unregisterDataPushListener(dataPushListener)

                return true

            } catch (e: Throwable) {

                recordException(e)
            }

            return false
        }

        return true
    }

    private val tag = "$identifier :: $environment :: ${getHashCode()} ::"

    override fun isBusy() = busy.get()

    override fun isInitialized() = initialized.get()

    override fun isNotInitialized() = !isInitialized()

    fun isEmpty() = list.isEmpty()

    fun isNotEmpty() = list.isNotEmpty()

    fun get(index: Int): T? {

        return list[index]
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

            doClear(onChange, callback)
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

        val from = "replaceAllAndFilter(from='$from')"

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

    private fun notifyChanged(onChange: OnChangeCompleted?, action: String) {

        if (onUi) {

            onUiThread {

                onChange?.onChange(action, true)
                this@ListWrapper.onChange?.onChange(action, true)
            }

        } else {

            onChange?.onChange(action, true)
            this@ListWrapper.onChange?.onChange(action, true)
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

            notifyChanged(onChange, "add")
        }

        notifyCallback(callback)
    }

    private fun doAddAll(

        what: Collection<T>,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (list.addAll(what)) {

            notifyChanged(onChange, "addAll.${what.size}")
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

                list.add(where, what)

                if (list.elementAt(where) == what) {

                    notifyChanged(onChange, "update.$where")
                }
            }


        } catch (e: Throwable) {

            recordException(e)
        }

        notifyCallback(callback)
    }

    private fun doClear(

        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        list.clear()

        if (list.isEmpty()) {

            notifyChanged(onChange, "clear")
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

                            notifyChanged(onChange, "addAllAndFilter.${what?.size ?: 0}")
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

                            val res = sync("filter.each") { callback ->

                                filter.filter(filtered.filteredItems, callback)
                            }

                            filtered.filteredItems.clear()
                            filtered.filteredItems.addAll(res?.filteredItems ?: emptyList())

                            filtered.changedCount.set(filtered.changedCount.get() + (res?.changedCount?.get() ?: 0))

                            if (!filtered.modified.get()) {

                                filtered.modified.set(res?.isModified() == true)
                            }
                        }

                        if (!modified) {

                            modified = filtered.modified.get()
                        }

                        changedCount += filtered.changedCount.get()

                        if (filtered != list) {

                            doClear {

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

            doClear {

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

                            notifyChanged(onChange, "purge")
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

            notifyChanged(onChange, "removeAll.${what.size}")
        }

        notifyCallback(callback)
    }

    private fun doRemove(

        what: T,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (list.remove(what)) {

            notifyChanged(onChange, "remove")
        }

        notifyCallback(callback)
    }

    private fun doRemove(

        index: Int,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        list.removeAt(index)?.let {

            notifyChanged(onChange, "remove.$index")
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

    private fun getCollection(): Collection<T?>? {

        try {

            return dataAccess?.obtain()

        } catch (e: Throwable) {

            recordException(e)
        }

        return null
    }
}