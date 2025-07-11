package com.redelf.commons.data.wrappers

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.redelf.commons.data.model.identifiable.Identifiable
import com.redelf.commons.destruction.delete.DeletionCheck
import com.redelf.commons.execution.Executor
import com.redelf.commons.filtering.Filter
import com.redelf.commons.logging.Console
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class ListWrapper<T>(

    from: Any,
    environment: String,

    @JsonProperty("onUi")
    @SerializedName("onUi")
    private val onUi: Boolean,

    @JsonProperty("list")
    @SerializedName("list")
    private var list: MutableList<T>

) {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    private val tag = "${from::class.simpleName} :: ${from.hashCode()} :: $environment :: " +
            "Data list hash = ${getHashCode()} ::"

    fun isEmpty() = list.isEmpty()

    fun isNotEmpty() = list.isNotEmpty()

    fun add(from: String, value: T, callback: (() -> Unit)? = null) {

        if (DEBUG.get()) Console.log("$tag add(value=$value) from '$from'")

        if (onUi) {

            Executor.UI.execute {

                list.add(value)

                callback?.let {

                    it()
                }
            }

        } else {

            list.add(value)

            callback?.let {

                it()
            }
        }
    }

    fun get(index: Int): T? {

        return list[index]
    }

    fun remove(from: String, index: Int, callback: (() -> Unit)? = null) {

        Console.warning("$tag remove(index=$index) from '$from'")

        if (onUi) {

            Executor.UI.execute {

                list.removeAt(index)

                callback?.let {

                    it()
                }
            }

        } else {

            list.removeAt(index)

            callback?.let {

                it()
            }
        }
    }

    fun remove(from: String, what: T, callback: (() -> Unit)? = null) {

        Console.warning("$tag remove(what='$what') from '$from'")

        if (onUi) {

            Executor.UI.execute {

                list.remove(what)

                callback?.let {

                    it()
                }
            }

        } else {

            list.remove(what)

            callback?.let {

                it()
            }
        }
    }

    fun update(from: String, what: T, where: Int, callback: (() -> Unit)? = null) {

        Console.log("$tag update(what='$what,where=$where) from '$from'")

        if (onUi) {

            Executor.UI.execute {

                list.remove(what)

                callback?.let {

                    it()
                }
            }

        } else {

            list.remove(what)

            callback?.let {

                it()
            }
        }
    }

    fun indexOf(what: T): Int {

        return list.indexOf(what)
    }

    fun removeAll(from: String, what: Collection<T>, callback: (() -> Unit)? = null) {

        Console.warning("$tag removeAll(index=$what) from '$from'")

        if (onUi) {

            Executor.UI.execute {

                list.removeAll(what)

                callback?.let {

                    it()
                }
            }

        } else {

            list.removeAll(what)

            callback?.let {

                it()
            }
        }
    }

    fun clear(from: String, callback: (() -> Unit)? = null) {

        Console.warning("$tag doClear() from '$from'")

        if (onUi) {

            Executor.UI.execute {

                list.clear()

                callback?.let {

                    it()
                }
            }

        } else {

            list.clear()

            callback?.let {

                it()
            }
        }
    }

    fun replaceAllAndFilter(

        what: Collection<T>,
        from: String,
        removeDeleted: Boolean = true,
        filters: List<Filter<T>> = emptyList(),
        callback: ((modified: Boolean, changedCount: Int) -> Unit)? = null
    ) {

        addAllAndFilter(

            what,
            "replaceAllAndFilter(from='$from')",
            true,
            removeDeleted,
            filters,
            callback
        )
    }

    fun addAllAndFilter(

        what: Collection<T>,
        from: String,
        replace: Boolean = false,
        removeDeleted: Boolean = true,
        filters: List<Filter<T>> = emptyList(),
        callback: ((modified: Boolean, changedCount: Int) -> Unit)? = null
    ) {

        fun doAdd() {

            val from = "addAllAndFilter(from='$from')"

            if (replace && what.isEmpty()) {

                clear(from)
            }

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

                removeAll(from, toRemove)
            }

            linked.forEach { linked ->

                if (!toUpdate.contains(linked) && !toRemove.contains(linked)) {

                    modified = true

                    add(from, linked)
                }
            }

            fun filter() {

                fun notify() {

                    callback?.let {

                        it(modified, changedCount)
                    }
                }

                if (filters.isEmpty()) {

                    notify()

                } else {

                    var filtered = list

                    filters.forEach { filter ->

                        filtered = filter.filter(filtered)
                    }

                    if (filtered != list) {

                        clear(from) {

                            addAll(filtered, from) {

                                notify()
                            }
                        }

                    } else {

                        notify()
                    }
                }
            }

            if (removeDeleted) {

                purge(from) {

                    filter()
                }

            } else {

                filter()
            }
        }

        if (onUi) {

            Executor.UI.execute {

                doAdd()
            }

        } else {

            doAdd()
        }
    }

    fun addAll(what: Collection<T>, from: String, callback: (() -> Unit)? = null) {

        if (DEBUG.get()) Console.log("$tag doClear() from '$from'")

        if (onUi) {

            Executor.UI.execute {

                list.addAll(what)

                callback?.let {

                    it()
                }
            }

        } else {

            list.addAll(what)

            callback?.let {

                it()
            }
        }
    }

    fun purge(from: String, callback: (() -> Unit)? = null) {

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

                removeAll("purge(from='$from')", toRemove) {

                    if (DEBUG.get()) Console.log("$tag END")
                }
            }

            callback?.let {

                it()
            }
        }

        if (onUi) {

            Executor.UI.execute {

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
}