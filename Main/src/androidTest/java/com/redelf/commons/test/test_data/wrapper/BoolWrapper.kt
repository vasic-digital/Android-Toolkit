package com.redelf.commons.test.test_data.wrapper

import com.google.gson.reflect.TypeToken
import com.redelf.commons.logging.Console
import java.lang.reflect.Type


class BoolWrapper(wrapped: Boolean) : TypeWrapper<Boolean>(wrapped) {

    constructor() : this(false)

    override fun failPartitionData(number: Int, error: Throwable) {

        Console.error(error)
    }

    override fun getClazz(): Class<BoolWrapper> {

        return BoolWrapper::class.java
    }

    override fun getPartitionType(number: Int): Type? {

        return object : TypeToken<Boolean?>() {}.type
    }
}