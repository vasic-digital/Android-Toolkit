package com.redelf.commons.partition

import java.lang.reflect.Type

interface Partitional {

    fun isPartitioningEnabled(): Boolean

    fun getPartitionCount(): Int

    fun getPartitionData(number: Int): Any?

    fun setPartitionData(number: Int, data: Any?): Boolean

    fun getPartitionType(number: Int): Type?
}