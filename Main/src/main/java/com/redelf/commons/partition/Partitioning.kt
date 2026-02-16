package com.redelf.commons.partition

import com.redelf.commons.data.type.Typed
import java.lang.reflect.Type

/*
* NOTE: Future enhancement - support differential writes (only persist changes, not full data).
*   Possible approaches: CopyOnWriteArrayList for change tracking, or a layered model
*   similar to Docker image layers where only deltas are written.
*/
interface Partitioning<T> : Typed<T> {

    fun isPartitioningEnabled(): Boolean

    fun isPartitioningParallelized(): Boolean

    fun getPartitionCount(): Int

    fun getPartitionData(number: Int): Any?

    fun isPartitionCollection(number: Int): Boolean? = null

    /*
        NOTE: Future enhancement - make partition data setting fully automatic with the possibility
        of override and automatic data conversion between partition format and domain types.
    */
    fun setPartitionData(number: Int, data: Any?): Boolean

    fun failPartitionData(number: Int, error: Throwable)

    /*
        NOTE: Future enhancement - make partition type resolution fully automatic
        with the possibility of override for custom type mappings.
    */
    fun getPartitionType(number: Int): Type?
}