package com.redelf.commons.test.test_data.wrapper

import com.redelf.commons.logging.Console
import java.util.concurrent.CopyOnWriteArrayList

class BoolListWrapper(list: CopyOnWriteArrayList<Boolean>) : TypeListWrapper<Boolean>(list) {

    constructor() : this(CopyOnWriteArrayList())

    override fun failPartitionData(number: Int, error: Throwable) {

        Console.error(error)
    }

    override fun getClazz(): Class<BoolListWrapper> {

        return BoolListWrapper::class.java
    }
}