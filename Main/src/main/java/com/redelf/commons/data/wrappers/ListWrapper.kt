package com.redelf.commons.data.wrappers

import com.redelf.commons.data.model.identifiable.Identifiable
import com.redelf.commons.destruction.delete.DeletionCheck
import com.redelf.commons.extensions.onUiThread
import com.redelf.commons.extensions.recordException
import com.redelf.commons.filtering.Filter
import com.redelf.commons.filtering.FilterResult
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.logging.Console
import com.redelf.commons.management.DataManagement
import com.redelf.commons.modification.OnChangeCompleted
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.state.BusyCheck
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ListWrapper<T>(

    from: Any,
    environment: String = "default",

    private val dataManager: Obtain<DataManagement<*>>? = null,

    @Transient
    private val onUi: Boolean,

    @Transient
    private var list: MutableList<T> = mutableListOf(),

    @Transient
    private var onChange: OnChangeCompleted? = null

) : BusyCheck, TerminationSynchronized {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    private val busy = AtomicBoolean()
    private val executor: ExecutorService = Executors.newFixedThreadPool(1)

    private val dataPushListener: OnObtain<Boolean?>? = if (

        dataManager != null &&
        onChange != null

    ) {

        object : OnObtain<Boolean?> {

            override fun onCompleted(data: Boolean?) {

                if (data == true) {

                    onChange?.onChange("dataPushed", true)
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

        dataPushListener?.let {

            try {

                dataManager?.obtain()?.register(it)

            } catch (e: Throwable) {

                recordException(e)
            }
        }
    }

    override fun terminate(vararg args: Any): Boolean {

        dataPushListener?.let {

            try {

                dataManager?.obtain()?.unregister(dataPushListener)

                return true

            } catch (e: Throwable) {

                recordException(e)
            }

            return false
        }

        return true
    }

    private val tag = "${from::class.simpleName} :: ${from.hashCode()} :: $environment :: " +
            "Data list hash = ${getHashCode()} ::"

    override fun isBusy() = busy.get()

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

        what: Collection<T>,
        from: String,
        removeDeleted: Boolean = true,
        filters: List<Filter<T>> = emptyList(),
        onChange: OnChangeCompleted? = null,
        callback: ((modified: Boolean, changedCount: Int) -> Unit)? = null

    ) {

        val from = "replaceAllAndFilter(from='$from')"

        exec {

            doAddAllAndFilter(what, from, true, removeDeleted, filters, onChange, callback)
        }
    }

    fun addAllAndFilter(

        what: Collection<T>,
        from: String,
        replace: Boolean = false,
        removeDeleted: Boolean = true,
        filters: List<Filter<T>> = emptyList(),
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

        what: Collection<T>,
        from: String,
        replace: Boolean = false,
        removeDeleted: Boolean = true,
        filters: List<Filter<T>> = emptyList(),
        onChange: OnChangeCompleted? = null,
        callback: ((modified: Boolean, changedCount: Int) -> Unit)? = null

    ) {

        if (DEBUG.get()) Console.log("doAddAllAndFilter(from='$from')")

        fun next() {

            var modified = false
            var changedCount = 0
            val linked = LinkedList(what)

            val toRemove = mutableListOf<T>()
            val toUpdate = mutableListOf<T>()

            list.forEach { wItem ->

                var where = -1
                var found: T? = null

                linked.forEach { lItem ->

                    if (found == null) {

                        if (lItem is Identifiable<*> && wItem is Identifiable<*>) {

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

                doRemoveAll( toRemove)
            }

            if (toUpdate.isNotEmpty()) {

                changedCount += toUpdate.size
            }

            linked.forEach { linked ->

                if (!toUpdate.contains(linked) && !toRemove.contains(linked)) {

                    modified = true

                    changedCount++

                    doAdd(linked)
                }
            }

            fun filter() {

                fun notify() {

                    if (modified) {

                        notifyChanged(onChange, "addAllAndFilter.${what.size}")
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

                        val res = filter.filter(filtered.filteredItems)

                        filtered.filteredItems.clear()
                        filtered.filteredItems.addAll(res.filteredItems)

                        filtered.changedCount.set(filtered.changedCount.get() + res.changedCount.get())

                        if (!filtered.modified.get()) {

                            filtered.modified.set(res.isModified())
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

                    Console.error("$tag Execute :: ERROR: ${e.message}")

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
}