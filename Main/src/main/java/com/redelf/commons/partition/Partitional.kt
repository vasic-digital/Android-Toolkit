package com.redelf.commons.partition

import com.redelf.commons.type.Typed
import java.lang.reflect.Type

interface Partitional<T> : Typed<T> {

    fun isPartitioningEnabled(): Boolean

    fun getPartitionCount(): Int

    fun getPartitionData(number: Int): Any?

    /*
        TODO: To be fully-automatic, with possibility of override and automatic data conversion
    */
    fun setPartitionData(number: Int, data: Any?): Boolean

    /*
        TODO: To be fully-automatic, with possibility of override
    */
    fun getPartitionType(number: Int): Type?
}