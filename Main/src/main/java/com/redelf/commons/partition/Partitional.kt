package com.redelf.commons.partition

interface Partitional {

    fun getPartitionCount(): Int

    fun getPartitionData(number: Int): Any?

    fun setPartitionData(number: Int, data: Any?): Boolean
}