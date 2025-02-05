package com.redelf.commons.test.test_data.wrapper

import com.redelf.commons.logging.Console
import java.util.concurrent.CopyOnWriteArrayList

class LongListWrapper(list: CopyOnWriteArrayList<Double>) : TypeListWrapper<Double>(list) {

    constructor() : this(CopyOnWriteArrayList())

    override fun failPartitionData(number: Int, error: Throwable) {

        Console.error(error)
    }

    override fun getClazz(): Class<LongListWrapper> {

        return LongListWrapper::class.java
    }
}