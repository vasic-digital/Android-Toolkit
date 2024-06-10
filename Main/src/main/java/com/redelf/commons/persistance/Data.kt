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
import java.util.Deque
import java.util.Queue
import java.util.Stack
import java.util.Vector
import java.util.concurrent.atomic.AtomicBoolean


class Data private constructor(private val facade: Facade) :

    ShutdownSynchronized,
    TerminationSynchronized,
    InitializationWithContext {

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
                            val written = facade.put(keyRow, value)

                            if (written) {

                                if (DEBUG.get()) Timber.v(

                                    "$tag WRITTEN: Partition no. $partition, Row no. $row"
                                )

                            } else {

                                Timber.e("$tag FAILURE: Partition no. $i, Row no. $row")
                            }

                            return written
                        }

                        fun rowWrite(partition: Int, row: Int, mapKey: Any?, value: Any?): Boolean {

                            if (mapKey == null) {

                                return true
                            }

                            if (value == null) {

                                return true
                            }

                            val keyRow = keyRow(key, partition, row)
                            val rowValue = Pair(mapKey, value)

                            val written = facade.put(keyRow, rowValue)

                            if (written) {

                                if (DEBUG.get()) Timber.v(

                                    "$tag WRITTEN: Partition no. $partition, Row no. $row, " +
                                            "Pair hash ${rowValue.hashCode()}"
                                )

                            } else {

                                Timber.e("$tag FAILURE: Partition no. $i, Row no. $row, " +
                                        "Pair hash ${rowValue.hashCode()}"
                                )
                            }

                            return false
                        }

                        val collection = partition is Collection<*> || partition is Map<*, *>

                        if (collection) {

                            when (partition) {

                                is List<*> -> {

                                    partition.forEachIndexed {

                                        index, value -> rowWrite(i, index, value)
                                    }
                                }

                                is Map<*, *> -> {

                                    var index = 0

                                    partition.forEach {

                                        key, value -> rowWrite(i, index, key, value)
                                        index++
                                    }
                                }

                                is Set<*> -> {

                                    partition.forEachIndexed {

                                        index, value -> rowWrite(i, index, value)
                                    }
                                }

                                is Queue<*> -> {

                                    partition.forEachIndexed {

                                        index, value -> rowWrite(i, index, value)
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

            /*
            * FIXME: Investigate this warning
            */
            return get(key, null)
        }

        return facade.get(key)
    }

    @Suppress("DEPRECATION")
    operator fun <T> get(key: String?, defaultValue: T?): T? {

        if (key == null || isEmpty(key)) {

            return defaultValue
        }

        val clazz = getType(key)
        val partitionsCount = getPartitionsCount(key)

        if (partitionsCount > 0) {

            val count = partitionsCount - 1

            val tag = "Get :: key = $key, T = '${clazz?.simpleName}' :: Partitional ::"

            if (DEBUG.get()) Timber.v("$tag START, Partitions = $partitionsCount")

            try {

                clazz?.newInstance()?.let {

                    if (DEBUG.get()) Timber.v("$tag INSTANTIATED")

                    if (it is Partitional<*>) {

                        if (DEBUG.get()) Timber.v("$tag IS PARTITIONAL")

                        for (i in 0..count) {

                            val type = it.getPartitionType(i)

                            type?.let { t ->

                                /*
                                * TODO: Instantiate rows
                                */
                                val partition = facade.getByType(keyPartition(key, i), t)

                                partition?.let { part ->

                                    if (DEBUG.get()) Timber.v("$tag Obtained: $i")

                                    val set = it.setPartitionData(i, part)

                                    if (set) {

                                        if (DEBUG.get()) Timber.v("$tag Set: $i")

                                    } else {

                                        Timber.e("$tag FAILURE: Not set: $i")

                                        return defaultValue
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

                        val rRemoved = facade.delete(keyRow(key, i, j))

                        if (rRemoved) {

                            if (DEBUG.get()) Timber.v(

                                "$tag REMOVED: Partition no. $i, Row no. $j"
                            )

                        } else {

                            Timber.e("$tag FAILURE: Partition no. $i, Row no. $j")
                        }
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

        return facade.get(keyRows(key, partition), 0)
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

    private fun keyRow(key: String, partition: Int, row: Int) = "$key.$partition.$row"

    private fun keyRows(key: String, partition: Int) = "$key.$partition.rows"
}

