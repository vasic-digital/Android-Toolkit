package com.redelf.commons.test.data

import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


class LongWrapper(wrapped: Long) : TypeWrapper<Long>(wrapped) {

    constructor() : this(0)

    override fun getClazz(): Class<LongWrapper> {

        return LongWrapper::class.java
    }

    override fun getPartitionType(number: Int): Type? {

        return object : TypeToken<Long?>() {}.type
    }
}