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
import kotlin.reflect.full.primaryConstructor


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

        if (value is Partitional && value.isPartitioningEnabled()) {

            val tag = "Partitional :: Put ::"

            val partitionsCount = value.getPartitionCount()

            if (DEBUG.get()) Timber.v("$tag START, Partitions = $partitionsCount")

            if (partitionsCount > 0) {

                val marked = facade.put(keyMarkPartitionalData(key), partitionsCount)

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

        if (key == null || isEmpty(key)) {

            return null
        }

        val count = getPartitionsCount(key)

        if (count > 0) {

            return get(key, null)
        }

        return facade.get(key)
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String?, defaultValue: T): T {

        if (key == null || isEmpty(key)) {

            return defaultValue
        }

        val partitionsCount = getPartitionsCount(key)

        if (partitionsCount > 0) {

            val count = partitionsCount - 1

            val tag = "Partitional :: Get ::"

            if (DEBUG.get()) Timber.v("$tag START, Partitions = $partitionsCount")

            try {

                T::class.primaryConstructor?.call()?.let {

                    if (DEBUG.get()) Timber.v("$tag INSTANTIATED")

                    val pInstance = (it as T)

                    if (DEBUG.get()) Timber.v("$tag CONVERTED TO: ${T::class.simpleName}")

                    if (pInstance is Partitional) {

                        if (DEBUG.get()) Timber.v("$tag Partitional")

                        for (i in 0..count) {

                            val type = pInstance.getPartitionType(i)

                            type?.let { t ->

                                val partition = facade.getByType(keyPartition(key, i), t)

                                partition?.let { part ->

                                    if (DEBUG.get()) Timber.v("$tag Obtained: $i")

                                    val set = pInstance.setPartitionData(i, part)

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

            val markRemoved = facade.delete(keyMarkPartitionalData(key))

            if (!markRemoved) {

                Timber.e("$tag ERROR: Could not un-mark partitional data")

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

        return facade.get(keyMarkPartitionalData(key), 0)
    }

    private fun keyPartition(key: String, index: Int) = "$key.$index"

    private fun keyMarkPartitionalData(key: String) = "$key.partitions"
}

