package com.redelf.commons.persistance

import android.content.Context
import com.redelf.commons.data.type.PairDataInfo
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.forClassName
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.extensions.yieldWhile
import com.redelf.commons.lifecycle.InitializationWithContext
import com.redelf.commons.lifecycle.ShutdownSynchronized
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.obtain.suspendable.Obtain
import com.redelf.commons.partition.Partitioning
import com.redelf.commons.persistance.base.Facade
import com.redelf.commons.registration.Registration
import com.redelf.commons.security.encryption.EncryptionListener
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.util.Queue
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


@Suppress("DEPRECATION")
class DataDelegate private constructor(private val facade: Facade) :

    ShutdownSynchronized,
    TerminationSynchronized,
    InitializationWithContext,
    Registration<EncryptionListener<String, String>>

{

    /*
     * TODO:
     *  - Give to delegate abstractions so we multiple data delegates could support when needed
     *  - Recursively partitioning - Each map or list member -> children
     *  - Parallelize reading
     *  - Annotations
     *  - Support for multiple data delegates (what would this mean - TBD)
     *  - Connection with provided RecyclerView (Adapters, ViewHolders, etc)
     *  - Data binding
     */

    companion object {

        val DEBUG = AtomicBoolean()

        fun instantiate(persistenceBuilder: PersistenceBuilder): DataDelegate {

            val facade = DefaultFacade.initialize(persistenceBuilder)

            return DataDelegate(facade)
        }
    }

    private val putActions = ConcurrentHashMap<String, Any?>()

    override fun shutdown(): Boolean {

        return facade.shutdown()
    }

    override fun terminate(vararg args: Any): Boolean {

        return facade.terminate(*args)
    }

    
    override fun initialize(ctx: Context) {

        return facade.initialize(ctx)
    }

    override fun register(subscriber: EncryptionListener<String, String>) {

        if (facade is DefaultFacade) {

            facade.register(subscriber)
        }
    }

    override fun unregister(subscriber: EncryptionListener<String, String>) {

        if (facade is DefaultFacade) {

            facade.unregister(subscriber)
        }
    }

    override fun isRegistered(subscriber: EncryptionListener<String, String>): Boolean {

        if (facade is DefaultFacade) {

            return facade.isRegistered(subscriber)
        }

        return false
    }

    fun isEncryptionEnabled() = (facade is DefaultFacade) && facade.isEncryptionEnabled()

    fun <T> put(key: String?, value: T): Boolean {

        if (key == null || isEmpty(key)) {

            return false
        }

        val obtain = object : Obtain<Boolean> {

            override fun obtain(): Boolean {

                val tag = "Partitioning :: Put :: Key = $key ::"

                if (putActions.contains(key)) {

                    putActions[key]?.let {

                        if (it.equals(value)) {

                            Console.warning("$tag Already writing this same value")

                            return true
                        }
                    }

                    Console.warning("$tag Already writing")
                }

                yieldWhile(

                    timeoutInMilliseconds = 10 * 1000

                ) {

                    putActions.contains(key)
                }

                if (putActions.contains(key)) {

                    Console.error("$tag ERROR: Still writing")

                    return false
                }

                putActions.put(key, value as Any)

                if (value is Partitioning<*> && value.isPartitioningEnabled()) {

                    val type = value.getClazz()
                    val partitionsCount = value.getPartitionCount()

                    if (DEBUG.get()) Console.log(

                        "$tag START :: Partitions count = $partitionsCount, " +
                                "Type = '${type.canonicalName?.forClassName()}'"
                    )

                    if (partitionsCount > 0) {

                        val marked = facade.put(keyPartitions(key), partitionsCount) &&
                                facade.put(keyType(key), type.canonicalName?.forClassName())

                        if (!marked) {

                            Console.error("$tag ERROR: Could not mark partitioning data")

                            return false
                        }

                        val success = AtomicBoolean(true)
                        val parallelized = value.isPartitioningParallelized()
                        val latchCount = if (parallelized) partitionsCount else 0
                        val partitioningLatch = CountDownLatch(latchCount)

                        for (i in 0..<partitionsCount) {

                            val partition = value.getPartitionData(i)

                            fun doPartition(

                                async: Boolean,
                                partition: Any?,
                                callback: OnObtain<Boolean>? = null

                            ) : Boolean {

                                try {

                                    val dTag = "$tag DO: Partition no. $i ::"

                                    if (DEBUG.get()) {

                                        Console.log("$dTag START")
                                    }

                                    val action = object : Obtain<Boolean> {

                                        override fun obtain(): Boolean {

                                            val oTag = "$dTag Obtain async ::"

                                            if (DEBUG.get()) {

                                                Console.log("$oTag START")
                                            }

                                            try {

                                                if (partition == null) {

                                                    callback?.onCompleted(true)

                                                    if (DEBUG.get()) {

                                                        Console.log("$oTag END :: Null partition")
                                                    }

                                                    return true
                                                }

                                                partition.let {

                                                    fun simpleWrite(): Boolean {

                                                        if (DEBUG.get()) {

                                                            Console.log("$oTag Do simple write")
                                                        }

                                                        val written = facade.put(keyPartition(key, i), it)

                                                        if (written) {

                                                            if (DEBUG.get()) {

                                                                Console.log(

                                                                    "$oTag WRITTEN: Partition no. $i"
                                                                )
                                                            }

                                                        } else {

                                                            val msg = "FAILURE: Partition no. $i not put"

                                                            val e = IOException(msg)

                                                            Console.error("$oTag ERROR: ${e.message}")
                                                            recordException(e)
                                                        }

                                                        if (DEBUG.get()) {

                                                            Console.log("$oTag END :: Written")
                                                        }

                                                        return written
                                                    }

                                                    fun rowWrite(

                                                        partition: Int,
                                                        row: Int,
                                                        value: Any?

                                                    ): Boolean {

                                                        if (value == null) {

                                                            if (DEBUG.get()) {

                                                                Console.log("$oTag END :: Null value")
                                                            }

                                                            return true
                                                        }

                                                        if (DEBUG.get()) {

                                                            Console.log(

                                                                "$oTag Do row write :: " +
                                                                        "Key = '$row'"
                                                            )
                                                        }

                                                        val keyRow = keyRow(key, partition, row)
                                                        val keyRowType = keyRowType(key, partition, row)

                                                        var savedFqName = false
                                                        val savedValue = facade.put(keyRow, value)
                                                        val fqName = value::class.java.canonicalName?.forClassName()

                                                        if (isNotEmpty(fqName)) {

                                                            savedFqName = facade.put(keyRowType, fqName)

                                                        } else {

                                                            val msg = "Failed to obtain canonical " +
                                                                    "name for the '$value', Log no. = 1"

                                                            val e = IOException(msg)

                                                            Console.error("$oTag ERROR: ${e.message}")
                                                            recordException(e)
                                                        }

                                                        val written = savedValue && savedFqName

                                                        if (written) {

                                                            if (DEBUG.get()) Console.log(

                                                                "$oTag WRITTEN: Partition no. $partition, " +
                                                                        "Row no. $row, Qualified name: $fqName"
                                                            )

                                                        } else {

                                                            val msg = "Partition no. $i failure write :: " +
                                                                    "Row no. = $row, " +
                                                                    "Qualified name = $fqName, " +
                                                                    "Saved value = $savedValue, " +
                                                                    "Saved Fq. name = $savedFqName, " +
                                                                    "Log no. = 1"

                                                            val e = IOException(msg)

                                                            Console.error("$oTag ERROR :: ${e.message}")
                                                            recordException(e)
                                                        }

                                                        return written
                                                    }

                                                    
                                                    fun rowWrite(

                                                        partition: Int,
                                                        row: Int,
                                                        mapKey: Any?,
                                                        value: Any?,
                                                        mapKeyType: Class<*>?,
                                                        valueType: Class<*>?

                                                    ): Boolean {

                                                        if (mapKey == null) {

                                                            if (DEBUG.get()) {

                                                                Console.log("$oTag END :: Null map key")
                                                            }

                                                            return true
                                                        }

                                                        if (DEBUG.get()) {

                                                            Console.log("$oTag Do map " +
                                                                    "row write :: " +
                                                                    "Map key = '$mapKey'")
                                                        }

                                                        if (value == null) {

                                                            if (DEBUG.get()) {

                                                                Console.log("$oTag END :: Null value / 2")
                                                            }

                                                            return true
                                                        }

                                                        if (mapKeyType == null) {

                                                            val msg = "FAILURE: Partition no. $i, " +
                                                                    "Row no. $row, No map key type provided"

                                                            val e = IOException(msg)

                                                            Console.error("$oTag ERROR: ${e.message}")
                                                            recordException(e)

                                                            return false
                                                        }

                                                        if (valueType == null) {

                                                            val msg = "FAILURE: Partition no. $i, " +
                                                                    "Row no. $row, No value type provided"

                                                            val e = IOException(msg)

                                                            Console.error("$oTag ERROR: ${e.message}")
                                                            recordException(e)

                                                            return false
                                                        }

                                                        val keyRow = keyRow(key, partition, row)
                                                        val keyRowType = keyRowType(key, partition, row)

                                                        var mapKeyValue: Any = mapKey
                                                        var valueValue: Any = value

                                                        if (mapKey is Number) {

                                                            mapKeyValue = mapKey.toDouble()
                                                        }

                                                        if (value is Number) {

                                                            valueValue = value.toLong()
                                                        }

                                                        val rowValue = PairDataInfo(

                                                            mapKeyValue,
                                                            valueValue,
                                                            mapKeyType.canonicalName?.forClassName(),
                                                            valueType.canonicalName?.forClassName()
                                                        )

                                                        var savedFqName = false
                                                        val savedValue = facade.put(keyRow, rowValue)
                                                        val fqName = rowValue::class.java.canonicalName?.forClassName()

                                                        if (isNotEmpty(fqName)) {

                                                            savedFqName = facade.put(keyRowType, fqName)

                                                        } else {

                                                            val msg = "Failed to obtain canonical " +
                                                                    "name for the '$value', Log no. = 2"

                                                            val e = IOException(msg)

                                                            Console.error("$oTag ERROR: ${e.message}")
                                                            recordException(e)
                                                        }

                                                        val written = savedValue && savedFqName

                                                        if (written) {

                                                            if (DEBUG.get()) Console.log(

                                                                "$oTag WRITTEN: Partition no. $partition, " +
                                                                        "Row no. $row, " +
                                                                        "Qualified name: $fqName, " +
                                                                        "Pair data info: $rowValue"
                                                            )

                                                            return true

                                                        } else {

                                                            val msg = "Partition no. $i failure write :: " +
                                                                    "Row no. = $row, " +
                                                                    "Qualified name = $fqName, " +
                                                                    "Pair data info = $rowValue, " +
                                                                    "Saved value = $savedValue, " +
                                                                    "Saved Fq. name = $savedFqName, " +
                                                                    "Log no. = 2"

                                                            val e = IOException(msg)

                                                            Console.error("$oTag ERROR :: ${e.message}")
                                                            recordException(e)
                                                        }

                                                        return false
                                                    }

                                                    var collection =
                                                        partition is Collection<*> ||
                                                                partition is Map<*, *>

                                                    value.isPartitionCollection(i)?.let {

                                                        if (it != collection) {

                                                            if (DEBUG.get()) {

                                                                Console.log(

                                                                    "$oTag Collection :: Override " +
                                                                            "= $collection -> $it"
                                                                )
                                                            }
                                                        }

                                                        collection = it
                                                    }

                                                    if (collection) {

                                                        if (DEBUG.get()) {

                                                            Console.log("$oTag Collection")
                                                        }

                                                        when (partition) {

                                                            is List<*> -> {

                                                                if (setRowsCount(key, i, partition.size)) {

                                                                    partition.forEachIndexed {

                                                                            index, value ->

                                                                        rowWrite(i, index, value)
                                                                    }

                                                                } else {

                                                                    val msg = "FAILURE: Writing rows count"
                                                                    Console.error("$oTag $msg")
                                                                    val e = IOException(msg)
                                                                    callback?.onFailure(e)

                                                                    return false
                                                                }
                                                            }

                                                            is Map<*, *> -> {

                                                                if (setRowsCount(key, i, partition.size)) {

                                                                    var index = 0

                                                                    try {

                                                                        partition.forEach { key, value ->

                                                                            key?.let { k ->
                                                                                value?.let { v ->

                                                                                    rowWrite(

                                                                                        partition = i,
                                                                                        row = index,
                                                                                        mapKey = k,
                                                                                        value = v,
                                                                                        mapKeyType = k::class.java,
                                                                                        valueType = v::class.java
                                                                                    )
                                                                                }
                                                                            }

                                                                            index++
                                                                        }

                                                                    } catch (e: OutOfMemoryError) {

                                                                        Console.error(

                                                                            "$oTag ${e.message}"
                                                                        )

                                                                        recordException(e)
                                                                        callback?.onFailure(e)
                                                                    }

                                                                } else {

                                                                    val msg = "FAILURE: Writing rows count"
                                                                    Console.error("$oTag $msg")
                                                                    val e = IOException(msg)
                                                                    callback?.onFailure(e)

                                                                    return false
                                                                }
                                                            }

                                                            is Set<*> -> {

                                                                if (setRowsCount(key, i, partition.size)) {

                                                                    partition.forEachIndexed {

                                                                            index, value ->

                                                                        rowWrite(i, index, value)
                                                                    }

                                                                } else {

                                                                    val msg = "FAILURE: Writing rows count"
                                                                    Console.error("$oTag $msg")
                                                                    val e = IOException(msg)
                                                                    callback?.onFailure(e)

                                                                    return false
                                                                }
                                                            }

                                                            is Queue<*> -> {

                                                                if (setRowsCount(key, i, partition.size)) {

                                                                    partition.forEachIndexed {

                                                                            index, value ->

                                                                        rowWrite(i, index, value)
                                                                    }

                                                                } else {

                                                                    val msg = "FAILURE: Writing rows count"
                                                                    Console.error("$oTag $msg")
                                                                    val e = IOException(msg)
                                                                    callback?.onFailure(e)

                                                                    return false
                                                                }
                                                            }

                                                            else -> {

                                                                if (simpleWrite()) {

                                                                    if (DEBUG.get()) {

                                                                        Console.log(

                                                                            "$oTag WRITTEN: " +
                                                                                    "Partition no. " +
                                                                                    "$partition " +
                                                                                    "(simple write " +
                                                                                    "/ 2)"
                                                                        )
                                                                    }

                                                                } else {

                                                                    val msg = "FAILURE: Simple write failed"
                                                                    Console.error("$oTag $msg")
                                                                    val e = IOException(msg)
                                                                    callback?.onFailure(e)

                                                                    return false
                                                                }
                                                            }
                                                        }

                                                    } else {

                                                        if (DEBUG.get()) {

                                                            Console.log("$oTag Not collection")
                                                        }

                                                        if (simpleWrite()) {

                                                            if (DEBUG.get()) {

                                                                Console.log("$oTag WRITTEN: Partition no. " +
                                                                        "$partition (simple write / 1)")
                                                            }

                                                        } else {

                                                            val msg = "FAILURE: Simple write failed"
                                                            Console.error("$tag $msg")
                                                            val e = IOException(msg)
                                                            callback?.onFailure(e)

                                                            Console.error("$oTag END :: Failed simple write / 2")

                                                            return false
                                                        }
                                                    }
                                                }

                                                if (DEBUG.get()) {

                                                    Console.log("$oTag END")
                                                }

                                                callback?.onCompleted(true)
                                                return true

                                            } catch (e: Exception) {

                                                Console.error("$oTag ERROR: ${e.message}")

                                                callback?.onFailure(e)
                                                return false
                                            }
                                        }
                                    }

                                    if (async) {

                                        exec(

                                            onRejected = { e ->

                                                Console.error("$dTag ERROR: ${e.message}")

                                                recordException(e)
                                            }

                                        ) {

                                            if (DEBUG.get()) {

                                                Console.log("$dTag Obtain async :: PRE-START")
                                            }

                                            val res = action.obtain()

                                            if (DEBUG.get()) {

                                                if (res) {

                                                    Console.log("$dTag Obtain async :: END :: OK")

                                                } else {

                                                    Console.log("$dTag Obtain async :: END :: FAILURE")
                                                }
                                            }
                                        }

                                    } else {

                                        if (DEBUG.get()) {

                                            Console.log("$dTag Obtain sync")
                                        }

                                        return action.obtain()
                                    }

                                } catch (e: Exception) {

                                    callback?.onFailure(e)

                                    return false
                                }

                                return true
                            }

                            val partitionCallback = object : OnObtain<Boolean> {

                                override fun onCompleted(data: Boolean) {

                                    if (!data) {

                                        success.set(false)
                                    }

                                    partitioningLatch.countDown()
                                }

                                override fun onFailure(error: Throwable) {

                                    Console.error(error)

                                    success.set(false)

                                    partitioningLatch.countDown()
                                }
                            }

                            if (parallelized) {

                                doPartition(true, partition, partitionCallback)

                            } else {

                                if (!doPartition(false, partition)) {

                                    success.set(false)
                                }
                            }
                        }

                        if (parallelized) {

                            try {

                                return partitioningLatch.await(60, TimeUnit.SECONDS) && success.get()

                            } catch (e: InterruptedException) {

                                Console.error("$tag ERROR: ${e.message}")

                                return false
                            }

                        } else {

                            return success.get()
                        }

                    } else {

                        Console.error("$tag END: No partitions reported")

                        return false
                    }
                }

                return facade.put(key, value)
            }
        }

        val obtained = obtain.obtain()

        putActions.remove(key)

        return obtained
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String?): T? {

        val tag = "Get :: Key = '$key' ::"

        if (key == null || isEmpty(key)) {

            return null
        }

        if (putActions.contains(key)) {

            if (DEBUG.get()) {

                Console.debug("$tag Writing in progress")
            }

            try {

                return putActions[key] as T?

            } catch (e: Exception) {

                Console.error("$tag ERROR: ${e.message}")
                recordException(e)

                return null
            }
        }

        val count = getPartitionsCount(key)

        if (count > 0) {

            if (DEBUG.get()) Console.log("$tag Partitioning :: START")

            return get<T?>(key = key, defaultValue = null)
        }

        return facade.get(key)
    }

    
    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    operator fun <T> get(key: String?, defaultValue: T?): T? {

        if (key == null || isEmpty(key)) {

            return defaultValue
        }

        val clazz = getType(key)
        val partitionsCount = getPartitionsCount(key)

        if (partitionsCount > 0) {

            val tag = "Partitioning :: Get :: key = $key, T = '${clazz?.simpleName}' :: "

            if (DEBUG.get()) Console.log("$tag START, Partitions = $partitionsCount")

            try {

                clazz?.newInstance()?.let { instance ->

                    if (DEBUG.get()) Console.log("$tag INSTANTIATED")

                    if (instance is Partitioning<*>) {

                        if (DEBUG.get()) Console.log("$tag IS PARTITIONING")

                        for (i in 0..<partitionsCount) {

                            if (DEBUG.get()) {

                                Console.log(

                                    "$tag DO: Partition no. $i"
                                )
                            }

                            val type = instance.getPartitionType(i)

                            type?.let { t ->

                                val rowsCount = getRowsCount(key, i)

                                if (rowsCount > 0) {

                                    val pt = t as ParameterizedType
                                    val inT = Class.forName(pt.rawType.typeName.forClassName())

                                    try {

                                        val partition = if (inT.canonicalName?.forClassName() == "java.util.List") {

                                            mutableListOf<Any?>()

                                        } else {

                                            inT.newInstance()
                                        }

                                        for (j in 0..<rowsCount) {

                                            val keyRow = keyRow(key, i, j)
                                            val keyRowType = keyRowType(key, i, j)
                                            val rowType = facade.get(keyRowType, "").forClassName()

                                            if (isEmpty(rowType)) {

                                                Console.error(

                                                    "$tag FAILURE: No row type :: Key =" +
                                                            " '$keyRowType', " +
                                                            "Partition = $i, Row = $j"
                                                )

                                                return defaultValue

                                            } else {

                                                if (DEBUG.get()) Console.log(

                                                    "$tag Row type: '$rowType'"
                                                )
                                            }

                                            var rowClazz: Class<*>? = null

                                            try {

                                                val simpleClass = getSimple(rowType)

                                                simpleClass?.let {

                                                    rowClazz = it
                                                }

                                                if (simpleClass == null) {

                                                    val rType = rowType.forClassName()

                                                    rowClazz = when(rType) {

                                                        "string",
                                                        "java.lang.String",
                                                        "kotlin.String" -> String::class.java

                                                        "int",
                                                        "java.lang.Integer",
                                                        "kotlin.Integer" -> Int::class.java

                                                        "long",
                                                        "java.lang.Long",
                                                        "kotlin.Long" -> Long::class.java

                                                        "float",
                                                        "java.lang.Float",
                                                        "kotlin.Float" -> Float::class.java

                                                        "double",
                                                        "java.lang.Double",
                                                        "kotlin.Double" -> Double::class.java

                                                        "bool",
                                                        "boolean",
                                                        "java.lang.Boolean",
                                                        "kotlin.Boolean" -> Boolean::class.java


                                                        else -> Class.forName(rType)
                                                    }


                                                }

                                            } catch (e: ClassNotFoundException) {

                                                Console.error(e)
                                            }

                                            rowClazz?.let { clz ->

                                                val obtained = facade.getByClass(keyRow, clz)

                                                obtained?.let { obt ->

                                                    when (partition) {

                                                        is MutableList<*> -> {

                                                            val vts = instantiate(

                                                                what = rowClazz,
                                                                arg = obt
                                                            )

                                                            (partition as MutableList<Any>).add(vts)
                                                        }

                                                        is MutableMap<*, *> -> {

                                                            if (obt is PairDataInfo) {

                                                                obt.first.let { first ->
                                                                    obt.second.let { second ->

                                                                        val clz1 = Class.forName(

                                                                            (obt.firstType ?: "").forClassName()
                                                                        )

                                                                        val clz2 = Class.forName(

                                                                            (obt.secondType ?: "").forClassName()
                                                                        )

                                                                        if (DEBUG.get()) Console.log(

                                                                            "$tag Row key type: '${clz1.simpleName}', " +
                                                                                    "Row value type: '${clz2.simpleName}'"
                                                                        )

                                                                        val kts = instantiate(

                                                                            what = clz1,
                                                                            arg = first
                                                                        )

                                                                        val vts = instantiate(

                                                                            what = clz2,
                                                                            arg = second
                                                                        )

                                                                        (partition as MutableMap<Any, Any>).put(
                                                                            kts,
                                                                            vts
                                                                        )
                                                                    }
                                                                }

                                                            } else {

                                                                Console.error(

                                                                    "$tag FAILURE: " +
                                                                            "Unsupported map child " +
                                                                            "type " +
                                                                            "'${obt::class.simpleName}'"
                                                                )

                                                                return defaultValue
                                                            }
                                                        }

                                                        is MutableSet<*> -> {

                                                            (partition as MutableSet<Any>).add(obt)
                                                        }

                                                        else -> {

                                                            Console.error(

                                                                "$tag FAILURE: Unsupported " +
                                                                        "partition type '${t.typeName}'"
                                                            )

                                                            return defaultValue
                                                        }
                                                    }
                                                }

                                                if (obtained == null) {

                                                    Console.error(

                                                        "$tag FAILURE: Obtained row is null"
                                                    )

                                                    return defaultValue
                                                }
                                            }

                                            if (rowClazz == null) {

                                                Console.error("$tag FAILURE: Row class is null")

                                                return defaultValue
                                            }
                                        }

                                        var set = false

                                        set = instance.setPartitionData(i, partition)

                                        if (set) {

                                            if (DEBUG.get()) {

                                                Console.log("$tag Set: $i")
                                            }

                                        } else {

                                            Console.error("$tag FAILURE: Not set: $i")

                                            return defaultValue
                                        }

                                    } catch (e: Exception) {

                                        Console.error(

                                            "$tag ERROR :: " +
                                                "Partition canonical name: ${inT.canonicalName?.forClassName()}"
                                        )

                                        instance.failPartitionData(i, e)
                                    }

                                } else {

                                    val partition = facade.getByType(keyPartition(key, i), t)

                                    partition?.let { part ->

                                        if (DEBUG.get()) Console.log("$tag Obtained: $i")

                                        val set = instance.setPartitionData(i, part)

                                        if (set) {

                                            if (DEBUG.get()) Console.log("$tag Set: $i")

                                        } else {

                                            Console.error("$tag FAILURE: Not set: $i")

                                            return defaultValue
                                        }
                                    }

                                    if (partition == null && DEBUG.get()) {

                                        Console.log("$tag WARNING: Null partition: $i")
                                    }
                                }
                            }

                            if (type == null) {

                                Console.error(

                                    "$tag FAILURE: No partition type " +
                                            "defined for partition: $i"
                                )

                                return defaultValue
                            }
                        }

                        return instance as T

                    } else {

                        Console.error("$tag END: No partitions reported")

                        return defaultValue
                    }
                }

            } catch (e: Exception) {

                Console.error("$tag ERROR: ${e.message}")

                recordException(e)
            }
        }

        return facade.get(key, defaultValue) ?: defaultValue
    }

    fun count(): Long = facade.count()

    
    fun delete(key: String?): Boolean {

        if (key == null || isEmpty(key)) {

            return false
        }

        val partitionsCount = getPartitionsCount(key)

        val tag = "Partitioning :: Delete ::"

        if (DEBUG.get()) Console.log("$tag START, Partitions = $partitionsCount")

        if (partitionsCount > 0) {

            val typeRemoved = facade.delete(keyType(key))
            val markRemoved = facade.delete(keyPartitions(key))

            if (!markRemoved) {

                Console.error("$tag ERROR: Could not un-mark partitioning data")

                return false
            }

            if (!typeRemoved) {

                Console.error("$tag ERROR: Could not un-mark type data")

                return false
            }

            for (i in 0..<partitionsCount) {

                val rowsCount = getRowsCount(key, i)
                val removed = facade.delete(keyPartition(key, i))

                if (rowsCount <= 0) {

                    if (removed) {

                        if (DEBUG.get()) Console.log("$tag REMOVED: Partition no. $i")

                    } else {

                        val msg = "FAILURE: Partition no. $i not removed, Log no. = 2"

                        val e = IOException(msg)

                        Console.error("$tag ERROR: ${e.message}")
                        recordException(e)
                    }

                } else {

                    for (j in 0..<rowsCount) {

                        val rRemoved = facade.delete(keyRow(key, i, j)) &&
                                facade.delete(keyRowType(key, i, j))

                        if (rRemoved) {

                            if (DEBUG.get()) Console.log(

                                "$tag REMOVED: Partition no. $i, Row no. $j"
                            )

                        } else {

                            val msg = "FAILURE: Partition no. $i not removed, Row no. $j"

                            val e = IOException(msg)

                            Console.error("$tag ERROR: ${e.message}")
                            recordException(e)
                        }
                    }

                    if (deleteRowsCount(key, i)) {

                        if (DEBUG.get()) Console.log(

                            "$tag REMOVED: Partition no. $i, Rows count deletion"
                        )

                    } else {

                        val msg = "FAILURE: Partition no. $i, Rows count deletion"

                        val e = IOException(msg)

                        Console.error("$tag ERROR: ${e.message}")
                        recordException(e)
                    }
                }
            }
        }

        return facade.delete(key)
    }

    
    operator fun contains(key: String?): Boolean {

        if (key == null || isEmpty(key)) {

            return false
        }

        val partitionsCount = getPartitionsCount(key)

        if (partitionsCount > 0) {

            return true
        }

        return facade.contains(key)
    }

    /*
         DANGER ZONE:
    */
    
    fun destroy() {

        facade.destroy()
    }

    
    fun deleteAll(): Boolean {

        return facade.deleteAll()
    }

    
    private fun getPartitionsCount(key: String): Int {

        return facade.get(keyPartitions(key), 0)
    }

    
    private fun getRowsCount(key: String, partition: Int): Int {

        val rowsKey = keyRows(key, partition)

        return facade.get(rowsKey, 0)
    }

    
    private fun setRowsCount(key: String, partition: Int, rows: Int): Boolean {

        val rowsKey = keyRows(key, partition)

        return facade.put(rowsKey, rows)
    }

    
    private fun deleteRowsCount(key: String, partition: Int): Boolean {

        val rowsKey = keyRows(key, partition)

        return facade.delete(rowsKey)
    }

    private fun getType(key: String): Class<*>? {

        val value = facade.get(keyType(key), "")

        try {

            return Class.forName(value.forClassName())

        } catch (e: ClassNotFoundException) {

            Console.error(e)
        }

        return null
    }

    private fun keyType(key: String) = "$key.type"

    private fun keyPartition(key: String, index: Int) = "$key.$index"

    private fun keyPartitions(key: String) = "$key.partitions"

    private fun keyRows(key: String, partition: Int) = "$key.$partition.rows"

    private fun keyRow(key: String, partition: Int, row: Int) = "$key.$partition.$row"

    private fun keyRowType(key: String, partition: Int, row: Int) = "$key.$partition.$row.type"

    @Throws(

        IllegalArgumentException::class,
        SecurityException::class,
        IllegalAccessException::class,
        InstantiationException::class

    )
    
    private fun instantiate(what: Class<*>?, arg: Any?): Any {

        arg?.let {

            if (it::class.java.canonicalName?.forClassName() == what?.canonicalName?.forClassName()) {

                return arg
            }
        }

        if (what == null) {

            throw IllegalArgumentException("The 'what' Class parameter is mandatory!")
        }

        if (isSimple(what)) {

            arg?.let {

                return it
            }
        }

        val tag = "Instantiate ::"

        if (DEBUG.get()) {

            Console.log("$tag '${what::class.java.canonicalName?.forClassName()}' from '${arg ?: "nothing"}'")
        }

        arg?.let { argument ->

            when (what) {

                UUID::class.java -> {

                    return UUID.fromString(arg.toString())
                }

                else -> {

                    what.constructors.forEach { constructor ->

                        val constructorIsValid = constructor.parameterCount == 1 &&
                                constructor.parameterTypes[0].canonicalName?.forClassName() ==
                                    argument::class.java.canonicalName?.forClassName()

                        if (constructorIsValid) {

                            return constructor.newInstance(argument)
                        }
                    }

                    val msg = "Constructor for the argument " +
                            "'${argument::class.java.canonicalName?.forClassName()}' " +
                            "not found to instantiate " +
                            "'${what.canonicalName?.forClassName()}'"

                    throw IllegalArgumentException(msg)
                }
            }
        }

        return what.newInstance()
    }

    
    private fun getSimple(type: String): Class<*>? {

        return when (type.forClassName()) {

            Float::class.java.canonicalName?.forClassName(),
            Int::class.java.canonicalName?.forClassName(),
            Long::class.java.canonicalName?.forClassName(),
            Short::class.java.canonicalName?.forClassName() -> {

                throw IllegalArgumentException(

                    "Not supported serialization type " +
                            "'$type', please use " +
                            "the " +
                            "'${Double::class.java.canonicalName?.forClassName()}'" +
                            " instead"
                )
            }

            Double::class.java.canonicalName?.forClassName() -> Double::class.java
            Boolean::class.java.canonicalName?.forClassName() -> Boolean::class.java
            Char::class.java.canonicalName?.forClassName() -> Char::class.java
            String::class.java.canonicalName?.forClassName() -> String::class.java
            Byte::class.java.canonicalName?.forClassName() -> Byte::class.java
            Array::class.java.canonicalName?.forClassName() -> Array::class.java

            else -> null
        }
    }

    private fun isSimple(clazz: Class<*>): Boolean {

        return when (clazz.canonicalName?.forClassName()) {

            java.lang.Long::class.java.canonicalName?.forClassName(),
            java.lang.Float::class.java.canonicalName?.forClassName(),
            java.lang.Integer::class.java.canonicalName?.forClassName(),
            java.lang.Short::class.java.canonicalName?.forClassName(),
            java.lang.Double::class.java.canonicalName?.forClassName(),
            java.lang.Boolean::class.java.canonicalName?.forClassName(),
            java.lang.Character::class.java.canonicalName?.forClassName(),
            java.lang.String::class.java.canonicalName?.forClassName(),

            Float::class.java.canonicalName?.forClassName(),
            Int::class.java.canonicalName?.forClassName(),
            Long::class.java.canonicalName?.forClassName(),
            Short::class.java.canonicalName?.forClassName(),
            Double::class.java.canonicalName?.forClassName(),
            Boolean::class.java.canonicalName?.forClassName(),
            Char::class.java.canonicalName?.forClassName(),
            String::class.java.canonicalName?.forClassName(),
            Byte::class.java.canonicalName?.forClassName(),
            Array::class.java.canonicalName?.forClassName() -> true

            else -> false
        }
    }
}

