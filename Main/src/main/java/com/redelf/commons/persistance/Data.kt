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
import java.util.concurrent.atomic.AtomicBoolean


class Data private constructor(private val facade: Facade) :

    ShutdownSynchronized,
    TerminationSynchronized,
    InitializationWithContext
{

    /*
     * TODO: If object is Partitional, each partition if is list or map, split in chunks
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

                val count = partitionsCount - 1;

                for (i in 0..count) {

                    val partition = value.getPartitionData(i)

                    partition?.let {

                        val written = facade.put(keyPartition(key, i), it)

                        if (written) {

                            if (DEBUG.get()) Timber.v("$tag WRITTEN: Partition no. $i")

                        } else {

                            Timber.e("$tag FAILURE: Partition no. $i")

                            return false
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

        val count = partitionsCount - 1

        if (DEBUG.get()) Timber.v("$tag START, Partitions = ${count + 1}")

        if (partitionsCount > 0) {

            val markRemoved = facade.delete(keyPartitions(key))
            val typeRemoved = facade.delete(keyType(key))

            if (!markRemoved) {

                Timber.e("$tag ERROR: Could not un-mark partitional data")

                return false
            }

            if (!typeRemoved) {

                Timber.e("$tag ERROR: Could not un-mark type data")

                return false
            }

            for (i in 0..count) {

                val removed = facade.delete(keyPartition(key, i))

                if (removed) {

                    if (DEBUG.get()) Timber.v("$tag REMOVED: Partition no. $i")

                } else {

                    Timber.e("$tag FAILURE: Partition no. $i")
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

