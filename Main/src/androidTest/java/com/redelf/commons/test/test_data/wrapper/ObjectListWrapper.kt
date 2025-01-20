package com.redelf.commons.test.test_data.wrapper

import com.redelf.commons.logging.Console
import com.redelf.commons.test.test_data.SampleData3
import java.util.concurrent.CopyOnWriteArrayList

class ObjectListWrapper(list: CopyOnWriteArrayList<SampleData3>) : TypeListWrapper<SampleData3>(list) {

    constructor() : this(CopyOnWriteArrayList())

    override fun failPartitionData(number: Int, error: Throwable) {

        Console.error(error)
    }

    override fun getClazz(): Class<ObjectListWrapper> {

        return ObjectListWrapper::class.java
    }
}