package com.redelf.commons.test.data

import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


class StringWrapper(wrapped: String) : TypeWrapper<String>(wrapped) {

    constructor() : this("")

    override fun getClazz(): Class<StringWrapper> {

        return StringWrapper::class.java
    }

    override fun getPartitionType(number: Int): Type? {

        return object : TypeToken<String?>() {}.type
    }
}