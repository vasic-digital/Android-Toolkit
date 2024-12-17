package com.redelf.commons.persistance

import android.content.Context
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import com.redelf.commons.data.type.PairDataInfo
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.forClassName
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.lifecycle.InitializationWithContext
import com.redelf.commons.lifecycle.ShutdownSynchronized
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.partition.Partitioning
import com.redelf.commons.persistance.base.Facade
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.util.Queue
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


@Suppress("DEPRECATION")
class DataDelegate private constructor(private val facade: Facade) :

    ShutdownSynchronized,
    TerminationSynchronized,
    InitializationWithContext

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

        @Throws(IllegalArgumentException::class)
        fun convert(what: ArrayList<String>): List<UUID> {

            val list = mutableListOf<UUID>()

            what.forEach {

                val uuid = UUID.fromString(it)
                list.add(uuid)
            }

            return list
        }
    }

    @Synchronized
    override fun shutdown(): Boolean {

        return facade.shutdown()
    }

    @Synchronized
    override fun terminate(vararg args: Any): Boolean {

        return facade.terminate(*args)
    }

    @Synchronized
    override fun initialize(ctx: Context) {

        return facade.initialize(ctx)
    }

    @Synchronized
    fun <T> put(key: String?, value: T): Boolean {

        if (key == null || isEmpty(key)) {

            return false
        }

        if (value is Partitioning<*> && value.isPartitioningEnabled()) {

            val tag = "Partitional :: Put ::"

            val type = value.getClazz()
            val partitionsCount = value.getPartitionCount()

            if (DEBUG.get()) Console.log(

                "$tag START, Partitions count = $partitionsCount, T = '${type.simpleName}'"
            )

            if (partitionsCount > 0) {

                val marked = facade.put(keyPartitions(key), partitionsCount) &&
                        facade.put(keyType(key), type.canonicalName)

                if (!marked) {

                    Console.error("$tag ERROR: Could not mark partitional data")

                    return false
                }

                val success = AtomicBoolean(true)
                val parallelized = value.isPartitioningParallelized()
                val latchCount = if (parallelized) partitionsCount else 0
                val partitioningLatch = CountDownLatch(latchCount)

                for (i in 0..<partitionsCount) {

                    val partition = value.getPartitionData(i)

                    @Synchronized
                    fun doPartition(

                        async: Boolean,
                        partition: Any?,
                        callback: OnObtain<Boolean>? = null

                    ) : Boolean {

                        try {

                            val action = object : Obtain<Boolean> {

                                override fun obtain(): Boolean {

                                    try {

                                        if (partition == null) {

                                            callback?.onCompleted(true)

                                            return true
                                        }

                                        partition.let {

                                            fun simpleWrite(): Boolean {

                                                val written = facade.put(keyPartition(key, i), it)

                                                if (written) {

                                                    if (DEBUG.get()) Console.log("$tag WRITTEN: Partition no. $i")

                                                } else {

                                                    Console.error("$tag FAILURE: Partition no. $i")
                                                }

                                                return written
                                            }

                                            fun rowWrite(

                                                partition: Int,
                                                row: Int,
                                                value: Any?

                                            ): Boolean {

                                                if (value == null) {

                                                    return true
                                                }

                                                val keyRow = keyRow(key, partition, row)
                                                val keyRowType = keyRowType(key, partition, row)

                                                val fqName = value::class.qualifiedName
                                                val savedValue = facade.put(keyRow, value)
                                                val savedFqName = facade.put(keyRowType, fqName)

                                                val written = savedValue && savedFqName

                                                if (written) {

                                                    if (DEBUG.get()) Console.log(

                                                        "$tag WRITTEN: Partition no. $partition, " +
                                                                "Row no. $row, Qualified name: $fqName"
                                                    )

                                                } else {

                                                    Console.error(
                                                        "$tag FAILURE: Partition no. $i, " +
                                                                "Row no. $row, Qualified name: $fqName"
                                                    )

                                                    if (!savedValue) {

                                                        Console.error("$tag Value has not been persisted")
                                                    }

                                                    if (!savedFqName) {

                                                        Console.error("$tag Qualified name has not been persisted")
                                                    }
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

                                                    return true
                                                }

                                                if (value == null) {

                                                    return true
                                                }

                                                if (mapKeyType == null) {

                                                    Console.error(
                                                        "$tag FAILURE: Partition no. $i, " +
                                                                "Row no. $row, No map key type provided"
                                                    )

                                                    return false
                                                }

                                                if (valueType == null) {

                                                    Console.error(
                                                        "$tag FAILURE: Partition no. $i, " +
                                                                "Row no. $row, No value type provided"
                                                    )

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
                                                    mapKeyType.canonicalName,
                                                    valueType.canonicalName
                                                )

                                                val fqName = rowValue::class.qualifiedName
                                                val savedValue = facade.put(keyRow, rowValue)
                                                val savedFqName = facade.put(keyRowType, fqName)

                                                val written = savedValue && savedFqName

                                                if (written) {

                                                    if (DEBUG.get()) Console.log(

                                                        "$tag WRITTEN: Partition no. $partition, " +
                                                                "Row no. $row, Qualified name: $fqName, " +
                                                                "Pair data info: $rowValue"
                                                    )

                                                } else {

                                                    Console.error(
                                                        "$tag FAILURE: Partition no. $i, " +
                                                                "Row no. $row, Qualified name: $fqName, " +
                                                                "Pair data info: $rowValue"
                                                    )

                                                    if (!savedValue) {

                                                        Console.error("$tag Value has not been persisted")
                                                    }

                                                    if (!savedFqName) {

                                                        Console.error("$tag Qualified name has not been persisted")
                                                    }
                                                }

                                                return false
                                            }

                                            val collection =
                                                partition is Collection<*> || partition is Map<*, *>

                                            if (collection) {

                                                when (partition) {

                                                    is List<*> -> {

                                                        if (setRowsCount(key, i, partition.size)) {

                                                            partition.forEachIndexed {

                                                                    index, value ->

                                                                rowWrite(i, index, value)
                                                            }

                                                        } else {

                                                            val msg = "FAILURE: Writing rows count"
                                                            Console.error("$tag $msg")
                                                            val e = IOException(msg)
                                                            callback?.onFailure(e)
                                                            return false
                                                        }
                                                    }

                                                    is Map<*, *> -> {

                                                        if (setRowsCount(key, i, partition.size)) {

                                                            var index = 0

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

                                                        } else {

                                                            val msg = "FAILURE: Writing rows count"
                                                            Console.error("$tag $msg")
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
                                                            Console.error("$tag $msg")
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
                                                            Console.error("$tag $msg")
                                                            val e = IOException(msg)
                                                            callback?.onFailure(e)
                                                            return false
                                                        }
                                                    }

                                                    else -> {

                                                        if (simpleWrite()) {

                                                            if (DEBUG.get()) {

                                                                Console.log("$tag Simple write OK")
                                                            }

                                                        } else {

                                                            val msg = "FAILURE: Simple write failed"
                                                            Console.error("$tag $msg")
                                                            val e = IOException(msg)
                                                            callback?.onFailure(e)
                                                            return false
                                                        }
                                                    }
                                                }

                                            } else {

                                                if (simpleWrite()) {

                                                    if (DEBUG.get()) {

                                                        Console.log("$tag Simple write OK")
                                                    }

                                                } else {

                                                    val msg = "FAILURE: Simple write failed"
                                                    Console.error("$tag $msg")
                                                    val e = IOException(msg)
                                                    callback?.onFailure(e)
                                                    return false
                                                }
                                            }
                                        }

                                        callback?.onCompleted(true)
                                        return true

                                    } catch (e: Exception) {

                                        callback?.onFailure(e)
                                        return false
                                    }
                                }
                            }

                            if (async) {

                                exec(

                                    onRejected = { e -> recordException(e) }

                                ) {

                                    action.obtain()
                                }

                            } else {

                                return action.obtain()
                            }

                        } catch (e: RejectedExecutionException) {

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

    operator fun <T> get(key: String?): T? {

        val tag = "Get :: key = $key, T = '${T::class.simpleName}' ::"

        if (key == null || isEmpty(key)) {

            return null
        }

        val count = getPartitionsCount(key)

        if (count > 0) {

            if (DEBUG.get()) Console.log("$tag Partitional :: START")

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

                            if (DEBUG.get()) Console.log("$tag Partition = $i")

                            val type = instance.getPartitionType(i)

                            type?.let { t ->

                                val rowsCount = getRowsCount(key, i)

                                if (rowsCount > 0) {

                                    val pt = t as ParameterizedType
                                    val inT = Class.forName(pt.rawType.typeName.forClassName())

                                    try {

                                        val partition = if (inT.canonicalName == "java.util.List") {

                                            mutableListOf<Any?>()

                                        } else {

                                            inT.newInstance()
                                        }

                                        for (j in 0..<rowsCount) {

                                            val keyRow = keyRow(key, i, j)
                                            val keyRowType = keyRowType(key, i, j)
                                            val rowType = facade.get(keyRowType, "")

                                            if (isEmpty(rowType)) {

                                                Console.error(

                                                    "$tag FAILURE: No row type for the key:" +
                                                            " '$keyRowType'"
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
                                                "Partition canonical name: ${inT.canonicalName}"
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

                                        Console.warning("$tag WARNING: Null partition: $i")
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

    @Synchronized
    fun delete(key: String?): Boolean {

        if (key == null || isEmpty(key)) {

            return false
        }

        val partitionsCount = getPartitionsCount(key)

        val tag = "Partitional :: Delete ::"

        if (DEBUG.get()) Console.log("$tag START, Partitions = $partitionsCount")

        if (partitionsCount > 0) {

            val typeRemoved = facade.delete(keyType(key))
            val markRemoved = facade.delete(keyPartitions(key))

            if (!markRemoved) {

                Console.error("$tag ERROR: Could not un-mark partitional data")

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

                        Console.error("$tag FAILURE: Partition no. $i")
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

                            Console.error("$tag FAILURE: Partition no. $i, Row no. $j")
                        }
                    }

                    if (deleteRowsCount(key, i)) {

                        if (DEBUG.get()) Console.log(

                            "$tag REMOVED: Partition no. $i, Rows count"
                        )

                    } else {

                        Console.error("$tag FAILURE: Partition no. $i, Rows count")
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
    @Synchronized
    fun destroy() {

        facade.destroy()
    }

    @Synchronized
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

    @Synchronized
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
    @Synchronized
    private fun instantiate(what: Class<*>?, arg: Any?): Any {

        arg?.let {

            if (it::class.java.canonicalName == what?.canonicalName) {

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

            Console.log("$tag '${what::class.qualifiedName}' from '${arg ?: "nothing"}'")
        }

        arg?.let { argument ->

            when (what) {

                UUID::class.java -> {

                    return UUID.fromString(arg.toString())
                }

                else -> {

                    what.constructors.forEach { constructor ->

                        val constructorIsValid = constructor.parameterCount == 1 &&
                                constructor.parameterTypes[0].canonicalName == argument::class.java.canonicalName

                        if (constructorIsValid) {

                            return constructor.newInstance(argument)
                        }
                    }

                    val msg = "Constructor for the argument " +
                            "'${argument::class.qualifiedName}' not found to instantiate " +
                            "'${what.canonicalName}'"

                    throw IllegalArgumentException(msg)
                }
            }
        }

        return what.newInstance()
    }

    @Synchronized
    private fun getSimple(type: String): Class<*>? {

        return when (type) {

            Float::class.qualifiedName,
            Int::class.qualifiedName,
            Long::class.qualifiedName,
            Short::class.qualifiedName -> {

                throw IllegalArgumentException(

                    "Not supported serialization type " +
                            "'$type', please use " +
                            "the " +
                            "'${Double::class.qualifiedName}'" +
                            " instead"
                )
            }

            Double::class.qualifiedName -> Double::class.java
            Boolean::class.qualifiedName -> Boolean::class.java
            Char::class.qualifiedName -> Char::class.java
            String::class.qualifiedName -> String::class.java
            Byte::class.qualifiedName -> Byte::class.java
            Array::class.qualifiedName -> Array::class.java

            else -> null
        }
    }

    private fun isSimple(clazz: Class<*>): Boolean {

        return when (clazz.canonicalName) {

            java.lang.Long::class.java.canonicalName,
            java.lang.Float::class.java.canonicalName,
            java.lang.Integer::class.java.canonicalName,
            java.lang.Short::class.java.canonicalName,
            java.lang.Double::class.java.canonicalName,
            java.lang.Boolean::class.java.canonicalName,
            java.lang.Character::class.java.canonicalName,
            java.lang.String::class.java.canonicalName,

            Float::class.java.canonicalName,
            Int::class.java.canonicalName,
            Long::class.java.canonicalName,
            Short::class.java.canonicalName,
            Double::class.java.canonicalName,
            Boolean::class.java.canonicalName,
            Char::class.java.canonicalName,
            String::class.java.canonicalName,
            Byte::class.java.canonicalName,
            Array::class.java.canonicalName -> true

            else -> false
        }
    }
}

