package com.redelf.commons.persistance

import android.content.Context
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.lifecycle.InitializationWithContext
import com.redelf.commons.lifecycle.ShutdownSynchronized
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.logging.Timber
import com.redelf.commons.partition.Partitional
import com.redelf.commons.persistance.base.Facade
import com.redelf.commons.type.PairDataInfo
import java.lang.reflect.ParameterizedType
import java.util.Queue
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.jvm.Throws


@Suppress("DEPRECATION")
class Data private constructor(private val facade: Facade) :

    ShutdownSynchronized,
    TerminationSynchronized,
    InitializationWithContext

{

    /*
     * TODO: Recursively partitioning - Each map or list member -> children
     */

    companion object {

        val DEBUG = AtomicBoolean()

        fun instantiate(persistenceBuilder: PersistenceBuilder): Data {

            val facade = DefaultFacade.initialize(persistenceBuilder)

            return Data(facade)
        }
    }

    override fun shutdown(): Boolean {

        return facade.shutdown()
    }

    override fun terminate(): Boolean {

        return facade.terminate()
    }

    override fun initialize(ctx: Context) {

        return facade.initialize(ctx)
    }

    fun <T> put(key: String?, value: T): Boolean {

        if (key == null || isEmpty(key)) {

            return false
        }

        if (value is Partitional<*> && value.isPartitioningEnabled()) {

            val tag = "Partitional :: Put ::"

            val type = value.getClazz()
            val partitionsCount = value.getPartitionCount()

            if (DEBUG.get()) Timber.v(

                "$tag START, Partitions count = $partitionsCount, T = '${type.simpleName}'"
            )

            if (partitionsCount > 0) {

                val marked = facade.put(keyPartitions(key), partitionsCount) &&
                        facade.put(keyType(key), type.canonicalName)

                if (!marked) {

                    Timber.e("$tag ERROR: Could not mark partitional data")

                    return false
                }

                for (i in 0..<partitionsCount) {

                    val partition = value.getPartitionData(i)

                    partition?.let {

                        fun simpleWrite(): Boolean {

                            val written = facade.put(keyPartition(key, i), it)

                            if (written) {

                                if (DEBUG.get()) Timber.v("$tag WRITTEN: Partition no. $i")

                            } else {

                                Timber.e("$tag FAILURE: Partition no. $i")
                            }

                            return written
                        }

                        fun rowWrite(partition: Int, row: Int, value: Any?): Boolean {

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

                                if (DEBUG.get()) Timber.v(

                                    "$tag WRITTEN: Partition no. $partition, " +
                                            "Row no. $row, Qualified name: $fqName"
                                )

                            } else {

                                Timber.e(
                                    "$tag FAILURE: Partition no. $i, " +
                                            "Row no. $row, Qualified name: $fqName"
                                )

                                if (!savedValue) {

                                    Timber.e("$tag Value has not been persisted")
                                }

                                if (!savedFqName) {

                                    Timber.e("$tag Qualified name has not been persisted")
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

                                Timber.e(
                                    "$tag FAILURE: Partition no. $i, " +
                                            "Row no. $row, No map key type provided"
                                )

                                return false
                            }

                            if (valueType == null) {

                                Timber.e(
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

                                if (DEBUG.get()) Timber.v(

                                    "$tag WRITTEN: Partition no. $partition, " +
                                            "Row no. $row, Qualified name: $fqName, " +
                                            "Pair data info: $rowValue"
                                )

                            } else {

                                Timber.e(
                                    "$tag FAILURE: Partition no. $i, " +
                                            "Row no. $row, Qualified name: $fqName, " +
                                            "Pair data info: $rowValue"
                                )

                                if (!savedValue) {

                                    Timber.e("$tag Value has not been persisted")
                                }

                                if (!savedFqName) {

                                    Timber.e("$tag Qualified name has not been persisted")
                                }
                            }

                            return false
                        }

                        val collection = partition is Collection<*> || partition is Map<*, *>

                        if (collection) {

                            when (partition) {

                                is List<*> -> {

                                    if (setRowsCount(key, i, partition.size)) {

                                        partition.forEachIndexed {

                                                index, value ->

                                            rowWrite(i, index, value)
                                        }

                                    } else {

                                        Timber.e("$tag FAILURE: Writing rows count")

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

                                        Timber.e("$tag FAILURE: Writing rows count")

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

                                        Timber.e("$tag FAILURE: Writing rows count")

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

                                        Timber.e("$tag FAILURE: Writing rows count")

                                        return false
                                    }
                                }

                                else -> {

                                    return simpleWrite()
                                }
                            }

                        } else {

                            return simpleWrite()
                        }
                    }
                }

            } else {

                Timber.e("$tag END: No partitions reported")

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

            if (DEBUG.get()) Timber.v("$tag Partitional :: START")

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

            val tag = "Get :: key = $key, T = '${clazz?.simpleName}' :: Partitional ::"

            if (DEBUG.get()) Timber.v("$tag START, Partitions = $partitionsCount")

            try {

                clazz?.newInstance()?.let { instance ->

                    if (DEBUG.get()) Timber.v("$tag INSTANTIATED")

                    if (instance is Partitional<*>) {

                        if (DEBUG.get()) Timber.v("$tag IS PARTITIONAL")

                        for (i in 0..<partitionsCount) {

                            val type = instance.getPartitionType(i)

                            type?.let { t ->

                                val rowsCount = getRowsCount(key, i)

                                if (rowsCount > 0) {

                                    val pt = t as ParameterizedType
                                    val inT = Class.forName(pt.rawType.typeName)

                                    val partition = inT.newInstance()

                                    for (j in 0..<rowsCount) {

                                        val keyRow = keyRow(key, i, j)
                                        val keyRowType = keyRowType(key, i, j)
                                        val rowType = facade.get(keyRowType, "")

                                        if (isEmpty(rowType)) {

                                            Timber.e(

                                                "$tag FAILURE: No row type for the key:" +
                                                        " '$keyRowType'"
                                            )

                                            return defaultValue

                                        } else {

                                            if (DEBUG.get()) Timber.v(

                                                "$tag Row type: '$rowType'"
                                            )
                                        }

                                        var rowClazz: Class<*>? = null

                                        fun getSimple(rType: String): Class<*>? {

                                            return when (rType) {

                                                Float::class.qualifiedName,
                                                Int::class.qualifiedName,
                                                Long::class.qualifiedName,
                                                Short::class.qualifiedName -> {

                                                    throw IllegalArgumentException(

                                                        "Not supported serialization type " +
                                                                "'$rType', please use " +
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

                                        try {

                                            val simpleClass = getSimple(rowType)

                                            simpleClass?.let {

                                                rowClazz = it
                                            }

                                            if (simpleClass == null) {

                                                rowClazz = Class.forName(rowType)
                                            }

                                        } catch (e: ClassNotFoundException) {

                                            Timber.e(e)
                                        }

                                        rowClazz?.let { clz ->

                                            val obtained = facade.getByClass(keyRow, clz)

                                            obtained?.let { obt ->

                                                when (partition) {

                                                    is MutableList<*> -> {

                                                        (partition as MutableList<Any>).add(obt)
                                                    }

                                                    is MutableMap<*, *> -> {

                                                        if (obt is PairDataInfo) {

                                                            obt.first.let { first ->
                                                                obt.second.let { second ->

                                                                    val clz1 = Class.forName(
                                                                        obt.firstType ?: ""
                                                                    )
                                                                    val clz2 = Class.forName(
                                                                        obt.secondType ?: ""
                                                                    )

                                                                    if (DEBUG.get()) Timber.v(

                                                                        "$tag Row key type: '${clz1.simpleName}', " +
                                                                                "Row value type: '${clz2.simpleName}'"
                                                                    )

                                                                    val kts = instantiate(what = clz1, arg = first)
                                                                    val vts = instantiate(what = clz2, arg = second)

                                                                    (partition as MutableMap<Any, Any>).put(kts, vts)
                                                                }
                                                            }

                                                        } else {

                                                            Timber.e(

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

                                                        Timber.e(

                                                            "$tag FAILURE: Unsupported " +
                                                                    "partition type '${t.typeName}'"
                                                        )

                                                        return defaultValue
                                                    }
                                                }
                                            }

                                            if (obtained == null) {

                                                Timber.e(

                                                    "$tag FAILURE: Obtained row is null"
                                                )

                                                return defaultValue
                                            }
                                        }

                                        if (rowClazz == null) {

                                            Timber.e("$tag FAILURE: Row class is null")

                                            return defaultValue
                                        }
                                    }

                                    val set = instance.setPartitionData(i, partition)

                                    if (set) {

                                        if (DEBUG.get()) {

                                            Timber.v("$tag Set: $i")

                                        } else {
                                        }

                                    } else {

                                        Timber.e("$tag FAILURE: Not set: $i")

                                        return defaultValue
                                    }

                                } else {

                                    val partition = facade.getByType(keyPartition(key, i), t)

                                    partition?.let { part ->

                                        if (DEBUG.get()) Timber.v("$tag Obtained: $i")

                                        val set = instance.setPartitionData(i, part)

                                        if (set) {

                                            if (DEBUG.get()) Timber.v("$tag Set: $i")

                                        } else {

                                            Timber.e("$tag FAILURE: Not set: $i")

                                            return defaultValue
                                        }
                                    }
                                }
                            }

                            if (type == null) {

                                Timber.e(

                                    "$tag FAILURE: No partition type " +
                                            "defined for partition: $i"
                                )

                                return defaultValue
                            }
                        }

                        return instance as T

                    } else {

                        Timber.e("$tag END: No partitions reported")

                        return defaultValue
                    }
                }

            } catch (e: Exception) {

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

        val tag = "Partitional :: Delete ::"

        if (DEBUG.get()) Timber.v("$tag START, Partitions = $partitionsCount")

        if (partitionsCount > 0) {

            val typeRemoved = facade.delete(keyType(key))
            val markRemoved = facade.delete(keyPartitions(key))

            if (!markRemoved) {

                Timber.e("$tag ERROR: Could not un-mark partitional data")

                return false
            }

            if (!typeRemoved) {

                Timber.e("$tag ERROR: Could not un-mark type data")

                return false
            }

            for (i in 0..<partitionsCount) {

                val rowsCount = getRowsCount(key, i)
                val removed = facade.delete(keyPartition(key, i))

                if (rowsCount <= 0) {

                    if (removed) {

                        if (DEBUG.get()) Timber.v("$tag REMOVED: Partition no. $i")

                    } else {

                        Timber.e("$tag FAILURE: Partition no. $i")
                    }

                } else {

                    for (j in 0..<rowsCount) {

                        val rRemoved = facade.delete(keyRow(key, i, j)) &&
                                facade.delete(keyRowType(key, i, j))

                        if (rRemoved) {

                            if (DEBUG.get()) Timber.v(

                                "$tag REMOVED: Partition no. $i, Row no. $j"
                            )

                        } else {

                            Timber.e("$tag FAILURE: Partition no. $i, Row no. $j")
                        }
                    }

                    if (deleteRowsCount(key, i)) {

                        if (DEBUG.get()) Timber.v(

                            "$tag REMOVED: Partition no. $i, Rows count"
                        )

                    } else {

                        Timber.e("$tag FAILURE: Partition no. $i, Rows count")
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

            return Class.forName(value)

        } catch (e: ClassNotFoundException) {

            Timber.e(e)
        }

        return null
    }

    private fun keyType(key: String) = "$key.type"

    private fun keyPartition(key: String, index: Int) = "$key.$index"

    private fun keyPartitions(key: String) = "$key.partitions"

    private fun keyRows(key: String, partition: Int) = "$key.$partition.rows"

    private fun keyRow(key: String, partition: Int, row: Int) = "$key.$partition.$row"

    private fun keyRowType(key: String, partition: Int, row: Int) = "$key.$partition.$row.type"

//    instantiate(what = clz1, arg = first)

    @Throws(

        IllegalArgumentException::class,
        SecurityException::class,
        IllegalAccessException::class,
        InstantiationException::class

    )
    private fun instantiate(what: Class<*>?, arg: Any?): Any {

        if (what == null) {

            throw IllegalArgumentException("The 'what' Class parameter is mandatory!")
        }

        val tag = "Instantiate ::"

        if (DEBUG.get()) {

            Timber.v("$tag '${what::class.qualifiedName}' from '${arg ?: "nothing"}'")
        }

        arg?.let { argument ->

            when (what) {

                UUID::class.java -> {

                    return UUID.fromString(arg.toString())
                }

                else -> return what.constructors.forEach { constructor ->

                    if (constructor.parameterCount == 1) {

                        return constructor.newInstance(argument)

                    } else {

                        val msg = "Constructor for the argument " +
                                "'${argument::class.qualifiedName}' not found to instantiate " +
                                "'${what.canonicalName}'"

                        throw IllegalArgumentException(msg)
                    }
                }
            }
        }

        return what.newInstance()
    }
}

