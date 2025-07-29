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
import com.redelf.commons.lifecycle.initialization.InitializedCheck
import com.redelf.commons.lifecycle.termination.TerminationSynchronized
import com.redelf.commons.logging.Console
import com.redelf.commons.management.DataManagement
import com.redelf.commons.management.DataPushResult
import com.redelf.commons.modification.OnChangeCompleted
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.obtain.ObtainParametrized
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.state.BusyCheck
import java.util.LinkedList
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/*
* TODO: Code cleanup and improvements needed ...
*/
open class ListWrapper<T, I, M : DataManagement<*>>(

    val identifier: String,
    val environment: String = "default",

    private val dataAccess: DataAccess<T, M>? = null,
    private val identifierObtainer: ObtainParametrized<I, T>,
    private val onUi: Boolean,
    private var onChange: OnChangeCompleted? = null,
    private val onDataPushed: OnObtain<DataPushResult?>? = null,
    private val defaultFilters: List<FilterAsync<T>> = emptyList(),

    /*
    * FIXME: Make this work properly and cover with the tests
    */
    trackPerItemChanges: Boolean = false,

    ) : BusyCheck, InitializedCheck, TerminationSynchronized {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    private val busy = AtomicBoolean()

    private val filteringInProgress = AtomicBoolean()
    private val changedIdentifiers = CopyOnWriteArraySet<I>()
    private val list: CopyOnWriteArraySet<T> = CopyOnWriteArraySet()
    private val lastCopy: CopyOnWriteArraySet<T> = CopyOnWriteArraySet()

    private val comparatorIdentifierObtainer = object : ObtainParametrized<I?, Int> {

        override fun obtain(param: Int): I? {

            val item = get(param)

            item?.let {

                return identifierObtainer.obtain(it)
            }

            return null
        }
    }

    private val comparator: CollectionChangesTracker<T, I>? = if (trackPerItemChanges) {

        CollectionChangesTracker(

            identifier,
            list,
            lastCopy,
            comparatorIdentifierObtainer,
            changedIdentifiers
        )

    } else null

    private val initialized = AtomicBoolean(dataAccess == null)
    private val executor: ExecutorService = Executors.newFixedThreadPool(1)

    private val dataPushListener: OnObtain<DataPushResult?>? = if (dataAccess != null) {

        object : OnObtain<DataPushResult?> {

            override fun onCompleted(data: DataPushResult?) {

                if (DEBUG.get()) {

                    Console.log("$tag Data push listener :: Push from = '$${data?.pushFrom}'")
                }

                if (data?.success == true) {

                    onDataPushed("dataPushListener.${data.pushFrom}", data)

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

            replaceAllAndFilter(items, from) { modified, count ->

                initialized.set(true)

                if (modified) {

                    if (DEBUG.get()) {

                        Console.log(

                            "$tag Init :: " +
                                    "Changes :: Detected :: Count=$count" +
                                    ", getCollection().count=${items?.size}"
                        )
                    }

                    notifyChanged(action = action)

                } else {

                    if (DEBUG.get()) {

                        Console.log(

                            "$tag Init :: " +
                                    "Changes :: None detected :: Count=$count" +
                                    ", getCollection().count=${items?.size}"
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

        comparator?.clear()

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

            doAdd(value, false, onChange, callback)
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

            doRemove(index, false, onChange, callback)
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

            doRemove(what, false, onChange, callback)
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

            doUpdate(what, where, false, onChange, callback)
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

            doRemoveAll(what, false, onChange, callback)
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
        skipNotifying: Boolean = false,
        filters: List<FilterAsync<T>> = defaultFilters,
        onChange: OnChangeCompleted? = null,
        callback: ((modified: Boolean, changedCount: Int) -> Unit)? = null

    ) {

        val from = "replaceAllAndFilter(from='$from',size=${what?.size ?: 0})"

        exec {

            if (what == null || what.isEmpty()) {

                val initSize = getSize()

                doClear(

                    from,
                    onChange,
                    false

                ) {

                    callback?.let {

                        it(initSize != 0, initSize)
                    }
                }

            } else {

                doAddAllAndFilter(

                    what,
                    from,
                    true,
                    removeDeleted,
                    skipNotifying,
                    filters,
                    onChange,
                    callback
                )
            }
        }
    }

    fun addAllAndFilter(

        what: Collection<T?>?,
        from: String,
        replace: Boolean = false,
        removeDeleted: Boolean = true,
        skipNotifying: Boolean = false,
        filters: List<FilterAsync<T>> = defaultFilters,
        onChange: OnChangeCompleted? = null,
        callback: ((modified: Boolean, changedCount: Int) -> Unit)? = null

    ) {

        exec {

            doAddAllAndFilter(what, from, replace, removeDeleted, skipNotifying, filters, onChange, callback)
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

            doAddAll(what, false, onChange, callback)
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

            doPurge(false, onChange, callback)
        }
    }

    private fun notifyChanged(

        onChange: OnChangeCompleted? = null,
        action: String

    ) {

        comparator?.run()

        if (DEBUG.get()) {

            Console.log(

                "Notify change :: Identifiers='${changedIdentifiers.toList()}'"
            )
        }

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
        skipNotifying: Boolean = false,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (!skipNotifying) {

            comparator?.makeCopy("doAdd")
        }

        if (list.add(value)) {

            if (!skipNotifying) {

                notifyChanged(onChange, "add")
            }
        }

        notifyCallback(callback)
    }

    private fun doAddAll(

        what: Collection<T>,
        skipNotifying: Boolean = false,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (!skipNotifying) {

            comparator?.makeCopy("doAddAll")
        }

        if (list.addAll(what)) {

            if (!skipNotifying) {

                notifyChanged(onChange, "addAll.${what.size}")
            }
        }

        notifyCallback(callback)
    }

    private fun doUpdate(

        what: T,
        where: Int,
        skipNotifying: Boolean = false,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        try {

            if (!skipNotifying) {

                comparator?.makeCopy("doUpdate")
            }

            if (list.size > where && list.removeAt(where) != null) {

                list.addAt(where, what)

                if (list.elementAt(where) == what) {

                    if (!skipNotifying) {

                        notifyChanged(onChange, "update.$where")
                    }
                }
            }


        } catch (e: Throwable) {

            recordException(e)
        }

        notifyCallback(callback)
    }

    private fun doUpdate(

        what: T,
        identifier: I,
        skipNotifying: Boolean = false,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        try {

            if (!skipNotifying) {

                comparator?.makeCopy("doUpdate")
            }

            val toRemove = mutableListOf<T>()
            val wId = identifierObtainer.obtain(what)

            list.forEach {

                val id = identifierObtainer.obtain(it)

                if (id == wId) {

                    toRemove.add(it)

                    if (DEBUG.get()) {

                        Console.log("$tag To remove on update :: Hash=${it.hashCode()}, Item=$it")
                    }
                }
            }

            if (toRemove.isNotEmpty()) {

                val removed = list.removeAll(toRemove)

                if (removed) {

                    if (DEBUG.get()) {

                        Console.log("$tag To remove on update :: Removed :: Count=${toRemove.size}")
                    }

                } else {

                    Console.error("$tag To remove on update :: Failed :: Count=${toRemove.size}")
                }
            }

            if (list.add(what)) {

                if (DEBUG.get()) {

                    Console.log("$tag To remove on update :: Added :: Hash=${what.hashCode()}, What=$what")
                }

                if (!skipNotifying) {

                    notifyChanged(onChange, "update.$identifier")
                }

            } else {

                Console.error("$tag To remove on update :: Not added :: Hash=${what.hashCode()}, What=$what")
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

        if (!skipNotifying) {

            comparator?.makeCopy("doClear")
        }

        list.clear()

        if (list.isEmpty()) {

            if (!skipNotifying) {

                notifyChanged(

                    onChange,
                    "clear(from='$from')",
                )
            }
        }

        notifyCallback(callback)
    }

    private fun doAddAllAndFilter(

        what: Collection<T?>?,
        from: String,
        replace: Boolean = false,
        removeDeleted: Boolean = true,
        skipNotifying: Boolean = false,
        filters: List<FilterAsync<T>> = defaultFilters,
        onChange: OnChangeCompleted? = null,
        callback: ((modified: Boolean, changedCount: Int) -> Unit)? = null

    ) {

        val from = "doAddAllAndFilter(from='$from',replace='$replace')"

        if (DEBUG.get()) Console.log("$tag $from")

        val doAddFrom = from

        fun next(nextFrom: String) {

            exec {

                var modified = false
                var changedCount = 0

                if (replace) {

                    modified = list.size != (what?.size ?: 0)

                    doClear(

                        from = from,
                        skipNotifying = true

                    ) {

                        val toAdd = mutableListOf<T>()

                        what?.forEach {

                            it?.let {

                                toAdd.add(it)
                            }
                        }

                        doAddAll(what = toAdd, skipNotifying = true)
                    }

                } else {

                    val linked = LinkedList(what ?: emptyList())

                    val toRemove = mutableListOf<T>()
                    val toUpdate = mutableListOf<T>()

                    list.forEach { wItem ->

                        var found: T? = null
                        var identifier: I? = null

                        linked.forEach { lItem ->

                            if (found == null) {

                                if (lItem is Number && wItem is Number) {

                                    if (lItem == wItem) {

                                        found = lItem

                                        if (lItem != wItem) {

                                            identifier = identifierObtainer.obtain(wItem)
                                        }
                                    }

                                } else if (lItem is Identifiable<*> && wItem is Identifiable<*>) {

                                    if (lItem.getId() == wItem.getId()) {

                                        found = lItem

                                        if (lItem != wItem) {

                                            identifier = identifierObtainer.obtain(wItem)
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

                            if (identifier != null) {

                                doUpdate(found, identifier, true)
                            }

                        } else {

                            toRemove.add(wItem)
                        }
                    }

                    if (toRemove.isNotEmpty()) {

                        modified = true

                        changedCount += toRemove.size

                        doRemoveAll(toRemove, true)
                    }

                    if (toUpdate.isNotEmpty()) {

                        changedCount += toUpdate.size
                    }

                    linked.forEach { linked ->

                        linked?.let { lkd ->

                            if (!toUpdate.contains(lkd) && !toRemove.contains(lkd)) {

                                modified = true

                                changedCount++

                                doAdd(lkd, true)
                            }
                        }
                    }
                }

                fun filter(filteredFrom: String) {

                    fun notify(from: String) {

                        if (modified) {

                            val action = "addAllAndFilter(from='$doAddFrom')" +
                                    ".next(from='$nextFrom')" +
                                    ".filter(from='$filteredFrom')" +
                                    ".notify(size=${what?.size ?: 0},from='$from')"

                            notifyChanged(

                                onChange,
                                action
                            )
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

                    doFilter(filters) {

                        if (!skipNotifying) {

                            notify("filter.end")
                        }
                    }
                }

                if (removeDeleted) {

                    doPurge(true) { purgedCount ->

                        if (purgedCount > 0) {

                            modified = true
                            changedCount += purgedCount
                        }

                        filter("doRemoveDeleted")
                    }

                } else {

                    filter("doNotRemoveDeleted")
                }
            }
        }

        comparator?.makeCopy("doAddAllAndFilter.${what?.size ?: 0}.${what.hashCode()}")

        if (replace) {

            doClear(

                "doAddAllAndFilter(from='$from," +
                        "filteredCount=NONE'," +
                        "originalCount=${list.size})",

                skipNotifying = true

            ) {

                next("doClear.completed")
            }

        } else {

            next("doNotClear")
        }
    }

    private fun doPurge(

        skipNotifying: Boolean = false,
        onChange: OnChangeCompleted? = null,
        callback: ((purgedCount: Int) -> Unit)? = null

    ) {

        val toRemove = mutableListOf<T>()

        list.forEach { message ->

            if (message is DeletionCheck && message.isDeleted()) {

                toRemove.add(message)
            }
        }

        if (toRemove.isNotEmpty()) {

            Console.debug("$tag Removing ${toRemove.size} items")

            doRemoveAll(

                toRemove,

                true,

                onChange = object : OnChangeCompleted {

                    override fun onChange(action: String, changed: Boolean) {

                        if (changed) {

                            if (!skipNotifying) {

                                notifyChanged(onChange, "purge")
                            }
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
        skipNotifying: Boolean = false,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (!skipNotifying) {

            comparator?.makeCopy("doRemoveAll")
        }

        if (list.removeAll(what)) {

            if (!skipNotifying) {

                notifyChanged(onChange, "removeAll.${what.size}")
            }
        }

        notifyCallback(callback)
    }

    private fun doRemove(

        what: T,
        skipNotifying: Boolean = false,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (!skipNotifying) {

            comparator?.makeCopy("doRemove.item")
        }

        if (list.remove(what)) {

            if (!skipNotifying) {

                notifyChanged(onChange, "remove")
            }
        }

        notifyCallback(callback)
    }

    private fun doRemove(

        index: Int,
        skipNotifying: Boolean = false,
        onChange: OnChangeCompleted? = null,
        callback: (() -> Unit)? = null

    ) {

        if (!skipNotifying) {

            comparator?.makeCopy("doRemove.$index")
        }

        list.removeAt(index)?.let {

            if (!skipNotifying) {

                notifyChanged(onChange, "remove.$index")
            }
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

    fun refresh(

        from: String,
        filters: List<FilterAsync<T>> = defaultFilters,
        callback: ((Boolean, Int) -> Unit)? = null

    ) {

        exec {

            Console.log("$tag Refresh :: From='$from'")

            val from = "refresh(from='$from')"
            val items = getCollection(from)

            replaceAllAndFilter(

                from = from,
                what = items,
                filters = filters

            ) { modified, count ->

                if (modified) {

                    notifyChanged(action = from)
                }

                callback?.let {

                    it(modified, count)
                }
            }
        }
    }

    private fun onDataPushed(pushContext: String, data: DataPushResult) {

        if (DEBUG.get()) {

            Console.log("On data pushed :: $pushContext :: Push from = '${data.pushFrom}'")
        }

        val items = getCollection("$pushContext(pushFrom='${data.pushFrom}')")

        val from =
            "$pushContext(pushFrom='${data.pushFrom}',size=${items?.size ?: 0})"

        onDataPushed?.onCompleted(data)

        replaceAllAndFilter(

            from = from,
            what = items,
            filters = defaultFilters

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

    private fun doFilter(

        filters: List<FilterAsync<T>> = defaultFilters,
        callback: () -> Unit

    ) {

        /*
         * FIXME:Solve the issue of parallel sorting with different sets of filters
         */
        if (filteringInProgress.get()) {

            yieldWhile {

                filteringInProgress.get()
            }

            callback()

            return
        }

        filteringInProgress.set(true)

        if (filters.isEmpty()) {

            callback()

        } else {

            filters.forEach { filter ->

                val res = sync(

                    debug = true,
                    context = "${identifier}.filter.each"

                ) { callback ->

                    filter.filter(list, callback)

                } == true

                if (res) {

                    callback()

                } else {

                    Console.error("$tag Failed to filter data")
                }
            }
        }

        filteringInProgress.set(false)
    }

    //    fun hasChangedAt(position: Int): Boolean {
    //
    //        val item = get(position)
    //
    //        item?.let {
    //
    //            val identifier = identifierObtainer.obtain(it)
    //            val changed = changedIdentifiers.contains(identifier)
    //
    //            if (DEBUG.get()) {
    //
    //                Console.log("Has changed at :: Position=$position")
    //            }
    //
    //            return changed
    //        }
    //
    //        return false
    //    }
}