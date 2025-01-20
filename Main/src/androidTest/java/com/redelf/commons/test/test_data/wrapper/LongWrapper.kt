package com.redelf.commons.test.test_data.wrapper

import com.google.gson.reflect.TypeToken
import com.redelf.commons.logging.Console
import java.lang.reflect.Type


class LongWrapper(wrapped: Long) : TypeWrapper<Long>(wrapped) {

    constructor() : this(0)

    override fun failPartitionData(number: Int, error: Throwable) {

        Console.error(error)
    }

    override fun getClazz(): Class<LongWrapper> {

        return LongWrapper::class.java
    }

    override fun getPartitionType(number: Int): Type? {

        return object : TypeToken<Long?>() {}.type
    }
}