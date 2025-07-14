package com.redelf.commons.data.wrappers

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.redelf.commons.data.model.identifiable.Identifiable
import com.redelf.commons.destruction.delete.DeletionCheck
import com.redelf.commons.extensions.recordException
import com.redelf.commons.extensions.syncUI
import com.redelf.commons.filtering.Filter
import com.redelf.commons.filtering.FilterResult
import com.redelf.commons.logging.Console
import com.redelf.commons.modification.OnChangeCompleted
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicBoolean

class ListWrapper<T>(

    from: Any,
    environment: String = "default",

    @JsonProperty("onUi")
    @SerializedName("onUi")
    private val onUi: Boolean,

    @JsonProperty("list")
    @SerializedName("list")
    private var list: MutableList<T>,

    @Transient
    private var onChange: OnChangeCompleted? = null

) {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    private val tag = "${from::class.simpleName} :: ${from.hashCode()} :: $environment :: " +
            "Data list hash = ${getHashCode()} ::"

    fun isEmpty() = list.isEmpty()

    fun isNotEmpty() = list.isNotEmpty()

    fun add(

        from: String,
        value: T,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (DEBUG.get()) Console.log("$tag add(value=$value), from='$from'")

        if (onUi) {

            syncUI("lisWrapper.add(from='$from')") {

                doAdd(value, onChange, callback)
            }

        } else {

            doAdd(value, onChange, callback)
        }
    }

    fun get(index: Int): T? {

        return list[index]
    }

    fun getLast(): T? {

        return list.last()
    }

    fun getFirst(): T? {

        return list.first()
    }

    fun remove(

        from: String,
        index: Int,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        Console.warning("$tag remove(index=$index), from='$from'")

        if (onUi) {

            syncUI("lisWrapper.remove.$index.(from='$from')") {

                doRemove(index, onChange, callback)
            }

        } else {

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

        if (onUi) {

            syncUI("lisWrapper.remove.item.(from='$from')") {

                doRemove(what, onChange, callback)
            }

        } else {

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

        if (onUi) {

            syncUI("lisWrapper.update.$where.(from='$from')") {

                doUpdate(what, where, onChange, callback)
            }

        } else {

            doUpdate(what, where, onChange, callback)
        }
    }

    fun indexOf(what: T): Int {

        return list.indexOf(what)
    }

    fun removeAll(

        from: String,
        what: Collection<T>,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        Console.warning("$tag removeAll(index=$what), from='$from'")

        if (onUi) {

            syncUI("lisWrapper.removeAll(from='$from')") {

                doRemoveAll(what, onChange, callback)
            }

        } else {

            doRemoveAll(what, onChange, callback)
        }
    }

    fun clear(

        from: String,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        Console.warning("$tag clear(), from='$from'")

        if (onUi) {

            syncUI("lisWrapper.clear(from='$from')") {

                doClear(onChange, callback)
            }

        } else {

            doClear()
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

        if (onUi) {

            syncUI("listWrapper.replaceAndFilter(from='$from')") {

                doAddAllAndFilter(what, from, true, removeDeleted, filters, onChange, callback)
            }

        } else {

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

        if (onUi) {

            syncUI("lisWrapper.addAllAndFilter(from='$from')") {

                doAddAllAndFilter(what, from, replace, removeDeleted, filters, onChange, callback)
            }

        } else {

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

        if (onUi) {

            syncUI("lisWrapper.addAll(from='$from')") {

                doAddAll(what, onChange, callback)
            }

        } else {

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

        if (onUi) {

            syncUI("lisWrapper.purge(from='$from')") {

                doPurge(onChange, callback)
            }

        } else {

            doPurge(onChange, callback)
        }
    }

    fun getHashCode(): Int {

        return list.hashCode()
    }

    fun getSize(): Int {

        return list.size
    }

    fun getList() = list.toList()

    fun contains(what: T) = list.contains(what)

    private fun notifyChanged(onChange: OnChangeCompleted?, action: String) {

        onChange?.onChange(action, true)
        this@ListWrapper.onChange?.onChange(action, true)
    }

    private fun doAdd(

        value: T,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (list.add(value)) {

            notifyChanged(onChange, "add")
        }

        callback?.let {

            it()
        }
    }

    private fun doAddAll(

        what: Collection<T>,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (list.addAll(what)) {

            notifyChanged(onChange, "addAll.${what.size}")
        }

        callback?.let {

            it()
        }
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

        callback?.let {

            it()
        }
    }

    private fun doClear(

        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        list.clear()

        if (list.isEmpty()) {

            notifyChanged(onChange, "clear")
        }

        callback?.let {

            it()
        }
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

                        it(modified, changedCount)
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

        callback?.let {

            it()
        }
    }

    private fun doRemove(

        what: T,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (list.remove(what)) {

            notifyChanged(onChange, "remove")
        }

        callback?.let {

            it()
        }
    }

    private fun doRemove(

        index: Int,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        list.removeAt(index)?.let {

            notifyChanged(onChange, "remove.$index")
        }

        callback?.let {

            it()
        }
    }
}