package com.redelf.commons.test.test_data.wrapper

import com.redelf.commons.logging.Console
import java.util.concurrent.CopyOnWriteArrayList

class StringListWrapper(list: CopyOnWriteArrayList<String>) : TypeListWrapper<String>(list) {

    constructor() : this(CopyOnWriteArrayList())

    override fun failPartitionData(number: Int, error: Throwable) {

        Console.error(error)
    }

    override fun getClazz(): Class<StringListWrapper> {

        return StringListWrapper::class.java
    }
}