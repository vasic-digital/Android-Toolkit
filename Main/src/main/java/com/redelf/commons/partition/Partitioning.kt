package com.redelf.commons.partition

import com.redelf.commons.data.type.Typed
import java.lang.reflect.Type

/*
* TODO: Make sure that is possible to write out only the differences (changes)
*   - Hint: CopyOnWriteArrayList
*   - Hint: How Docker images work
*/
interface Partitioning<T> : Typed<T> {

    fun isPartitioningEnabled(): Boolean

    fun isPartitioningParallelized(): Boolean

    fun getPartitionCount(): Int

    fun getPartitionData(number: Int): Any?

    fun isPartitionCollection(number: Int): Boolean? = null

    /*
        TODO: To be fully-automatic, with possibility of override and automatic data conversion
    */
    fun setPartitionData(number: Int, data: Any?): Boolean

    fun failPartitionData(number: Int, error: Throwable)

    /*
        TODO: To be fully-automatic, with possibility of override
    */
    fun getPartitionType(number: Int): Type?
}