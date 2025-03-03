package com.redelf.commons.test.test_data.wrapper

import com.google.gson.reflect.TypeToken
import com.redelf.commons.logging.Console
import java.lang.reflect.Type


class StringWrapper(wrapped: String) : TypeWrapper<String>(wrapped) {

    constructor() : this("")

    override fun failPartitionData(number: Int, error: Throwable) {

        Console.error(error)
    }

    override fun getClazz(): Class<StringWrapper> {

        return StringWrapper::class.java
    }

    override fun getPartitionType(number: Int): Type? {

        return object : TypeToken<String?>() {}.type
    }
}