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
    environment: String,

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

    private val wrapperContext = "list.wrapper.$from.environment"

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

        fun doAdd() {

            if (list.add(value)) {

                notifyChanged(onChange, "add")
            }

            callback?.let {

                it()
            }
        }

        if (onUi) {

            syncUI(wrapperContext) {

                doAdd()
            }

        } else {

            doAdd()
        }
    }

    fun get(index: Int): T? {

        return list[index]
    }

    fun remove(

        from: String,
        index: Int,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        Console.warning("$tag remove(index=$index), from='$from'")

        fun doRemove() {

            list.removeAt(index)?.let {

                notifyChanged(onChange, "remove.$index")
            }

            callback?.let {

                it()
            }
        }

        if (onUi) {

            syncUI(wrapperContext) {

                doRemove()
            }

        } else {

            doRemove()
        }
    }

    fun remove(

        from: String,
        what: T,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        Console.warning("$tag remove(what='$what'), from='$from'")

        fun doRemove() {

            if (list.remove(what)) {

                notifyChanged(onChange, "remove")
            }

            callback?.let {

                it()
            }
        }

        if (onUi) {

            syncUI(wrapperContext) {

                doRemove()
            }

        } else {

            doRemove()
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

        fun doUpdate() {

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

        if (onUi) {

            syncUI(wrapperContext) {

                doUpdate()
            }

        } else {

            doUpdate()
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

        fun doRemoveAll() {

            if (list.removeAll(what)) {

                notifyChanged(onChange, "removeAll.${what.size}")
            }

            callback?.let {

                it()
            }
        }

        if (onUi) {

            syncUI(wrapperContext) {

                doRemoveAll()
            }

        } else {

            list.removeAll(what)

            doRemoveAll()
        }
    }

    fun clear(

        from: String,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null,

        ) {

        Console.warning("$tag doClear(), from='$from'")

        fun doClear() {

            list.clear()

            if (list.isEmpty()) {

                notifyChanged(onChange, "clear")
            }

            callback?.let {

                it()
            }


        }

        if (onUi) {

            syncUI(wrapperContext) {

                doClear()
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

        addAllAndFilter(

            what,
            "replaceAllAndFilter(from='$from')",
            true,
            removeDeleted,
            filters,
            onChange,
            callback
        )
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

        fun doAdd() {

            val from = "addAllAndFilter(from='$from')"

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
                            }
                        }
                    }

                    if (found != null) {

                        toUpdate.add(found)

                        if (where > -1) {

                            update(from, found, where)
                        }

                    } else {

                        toRemove.add(wItem)
                    }
                }

                if (toRemove.isNotEmpty()) {

                    modified = true

                    changedCount += toRemove.size

                    removeAll(from, toRemove)
                }

                if (toUpdate.isNotEmpty()) {

                    changedCount += toUpdate.size
                }

                linked.forEach { linked ->

                    if (!toUpdate.contains(linked) && !toRemove.contains(linked)) {

                        modified = true

                        changedCount++

                        add(from, linked)
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

                        val filtered = FilterResult(filteredItems = list)

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

                            clear(from) {

                                addAll(filtered.filteredItems, from) {

                                    notify()
                                }
                            }

                        } else {

                            notify()
                        }
                    }
                }

                if (removeDeleted) {

                    purge(from) { purgedCount ->

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

            if (replace && what.isEmpty()) {

                clear(from) {

                    next()
                }

            } else {

                next()
            }
        }

        if (onUi) {

            syncUI(wrapperContext) {

                doAdd()
            }

        } else {

            doAdd()
        }
    }

    fun addAll(

        what: Collection<T>,
        from: String,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (DEBUG.get()) Console.log("$tag addAll(), from='$from'")

        fun addAll() {

            if (list.addAll(what)) {

                notifyChanged(onChange, "addAll.${what.size}")
            }

            callback?.let {

                it()
            }
        }

        if (onUi) {

            syncUI(wrapperContext) {

                addAll()
            }

        } else {

            addAll()
        }
    }

    fun purge(

        from: String,
        onChange: OnChangeCompleted? = null,
        callback: ((purgedCount: Int) -> Unit)? = null

    ) {

        val tag = "$tag purge(from='$from')"

        Console.log("$tag START")

        fun doPurge() {

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

                removeAll(

                    "purge(from='$from')",
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

        if (onUi) {

            syncUI(wrapperContext) {

                doPurge()
            }

        } else {

            doPurge()
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
}